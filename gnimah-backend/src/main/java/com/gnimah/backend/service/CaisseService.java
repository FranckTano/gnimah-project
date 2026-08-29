package com.gnimah.backend.service;

import com.gnimah.backend.dto.caisse.*;
import com.gnimah.backend.entity.Caisse;
import com.gnimah.backend.entity.MouvementCaisse;
import com.gnimah.backend.entity.SessionCaisse;
import com.gnimah.backend.entity.Utilisateur;
import com.gnimah.backend.entity.enums.*;
import com.gnimah.backend.exception.BusinessException;
import com.gnimah.backend.exception.ResourceNotFoundException;
import com.gnimah.backend.repository.CaisseRepository;
import com.gnimah.backend.repository.MouvementCaisseRepository;
import com.gnimah.backend.repository.SessionCaisseRepository;
import com.gnimah.backend.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Tenue des caisses de la résidence.
 *
 * Ce service est le point de passage obligé de tout mouvement d'argent : les
 * autres services (paiements, réservations, dépenses, restauration, versements)
 * n'écrivent jamais directement dans le journal, ils appellent
 * {@link #enregistrer}. C'est ce qui garantit que le solde d'une caisse reflète
 * réellement la somme de ses mouvements.
 */
@Service
@RequiredArgsConstructor
public class CaisseService {

    private final CaisseRepository caisseRepository;
    private final SessionCaisseRepository sessionRepository;
    private final MouvementCaisseRepository mouvementRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final AuditService auditService;

    // ------------------------------------------------------------------
    // Référentiel des caisses
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<CaisseResponse> findAll() {
        return caisseRepository.findByActifTrueOrderByNomAsc().stream()
                .map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CaisseResponse getById(Long id) {
        return toResponse(findCaisse(id));
    }

    @Transactional
    public CaisseResponse create(CaisseRequest request) {
        caisseRepository.findByNom(request.getNom()).ifPresent(c -> {
            throw new BusinessException("Une caisse porte déjà le nom « " + request.getNom() + " »");
        });
        Caisse caisse = Caisse.builder()
                .nom(request.getNom())
                .type(parseType(request.getType()))
                .description(request.getDescription())
                .responsable(request.getResponsableId() != null
                        ? utilisateurRepository.findById(request.getResponsableId()).orElse(null) : null)
                .actif(request.getActif() == null || request.getActif())
                .build();
        return toResponse(caisseRepository.save(caisse));
    }

    @Transactional
    public CaisseResponse update(Long id, CaisseRequest request) {
        Caisse caisse = findCaisse(id);
        caisse.setNom(request.getNom());
        caisse.setType(parseType(request.getType()));
        caisse.setDescription(request.getDescription());
        caisse.setResponsable(request.getResponsableId() != null
                ? utilisateurRepository.findById(request.getResponsableId()).orElse(null) : null);
        if (request.getActif() != null) caisse.setActif(request.getActif());
        return toResponse(caisseRepository.save(caisse));
    }

    /**
     * Caisse à utiliser pour une opération. Sans précision, la première caisse
     * de réception active fait office de caisse par défaut — c'est celle où
     * atterrissent les encaissements de séjour.
     */
    @Transactional(readOnly = true)
    public Caisse resoudreCaisse(Long caisseId) {
        if (caisseId != null) {
            return findCaisse(caisseId);
        }
        return caisseRepository.findFirstByTypeAndActifTrueOrderByIdAsc(TypeCaisse.RECEPTION)
                .or(() -> caisseRepository.findByActifTrueOrderByNomAsc().stream().findFirst())
                .orElseThrow(() -> new BusinessException(
                        "Aucune caisse active n'est configurée — créez-en une avant d'encaisser."));
    }

    // ------------------------------------------------------------------
    // Sessions (vacations)
    // ------------------------------------------------------------------

    @Transactional
    public SessionCaisseResponse ouvrir(OuvertureCaisseRequest request) {
        Caisse caisse = findCaisse(request.getCaisseId());
        sessionRepository.findFirstByCaisseIdAndStatutOrderByDateOuvertureDesc(caisse.getId(), StatutSessionCaisse.OUVERTE)
                .ifPresent(s -> {
                    throw new BusinessException("La caisse « " + caisse.getNom() + " » a déjà une session ouverte"
                            + (s.getCaissier() != null ? " par " + s.getCaissier().getNomComplet() : "") + ".");
                });

        Utilisateur agent = getAgentConnecte();
        BigDecimal fond = request.getFondCaisse() != null ? request.getFondCaisse() : BigDecimal.ZERO;

        SessionCaisse session = SessionCaisse.builder()
                .caisse(caisse)
                .caissier(agent)
                .dateOuverture(LocalDateTime.now())
                .fondCaisse(fond)
                .montantTheorique(fond)
                .statut(StatutSessionCaisse.OUVERTE)
                .notes(request.getNotes())
                .build();
        SessionCaisse saved = sessionRepository.save(session);

        // Le fond de caisse est une mise en place, pas une recette : il figure au
        // journal pour expliquer le solde, mais il est exclu de l'assiette des
        // versements (voir MouvementCaisseRepository.sumRecettePeriode).
        if (fond.compareTo(BigDecimal.ZERO) > 0) {
            enregistrerInterne(caisse, saved, SensMouvement.ENTREE, TypeMouvementCaisse.FOND_DE_CAISSE,
                    fond, ModePaiement.ESPECES, "Fond de caisse — ouverture de vacation",
                    null, "SESSION_CAISSE", saved.getId(), agent, null);
        }

        auditService.log(agent, "OUVERTURE_CAISSE", "SessionCaisse", saved.getId(),
                "Ouverture de " + caisse.getNom() + " avec un fond de " + fond + " FCFA");
        return toResponse(saved);
    }

    @Transactional
    public SessionCaisseResponse fermer(Long sessionId, FermetureCaisseRequest request) {
        SessionCaisse session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session de caisse", sessionId));
        if (session.getStatut() == StatutSessionCaisse.FERMEE) {
            throw new BusinessException("Cette session de caisse est déjà fermée");
        }
        if (request.getMontantCompte() == null) {
            throw new BusinessException("Le montant compté en caisse est requis pour fermer la vacation");
        }

        BigDecimal entrees = mouvementRepository.sumBySessionAndSens(sessionId, SensMouvement.ENTREE);
        BigDecimal sorties = mouvementRepository.sumBySessionAndSens(sessionId, SensMouvement.SORTIE);
        BigDecimal entreesEspeces = mouvementRepository.sumEspecesBySessionAndSens(sessionId, SensMouvement.ENTREE);
        BigDecimal sortiesEspeces = mouvementRepository.sumEspecesBySessionAndSens(sessionId, SensMouvement.SORTIE);

        // Le fond de caisse est déjà compté dans les entrées en espèces : le
        // rajouter au théorique le compterait deux fois.
        BigDecimal theorique = entreesEspeces.subtract(sortiesEspeces);
        BigDecimal ecart = request.getMontantCompte().subtract(theorique);

        session.setTotalEntrees(entrees);
        session.setTotalSorties(sorties);
        session.setMontantTheorique(theorique);
        session.setMontantCompte(request.getMontantCompte());
        session.setEcart(ecart);
        session.setStatut(StatutSessionCaisse.FERMEE);
        session.setDateFermeture(LocalDateTime.now());
        session.setFermePar(getAgentConnecte());
        if (request.getNotes() != null && !request.getNotes().isBlank()) {
            session.setNotes(request.getNotes());
        }

        SessionCaisse saved = sessionRepository.save(session);
        auditService.log(session.getFermePar(), "FERMETURE_CAISSE", "SessionCaisse", saved.getId(),
                "Fermeture de " + session.getCaisse().getNom() + " — théorique " + theorique
                        + ", compté " + request.getMontantCompte() + ", écart " + ecart + " FCFA");
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public SessionCaisseResponse sessionOuverte(Long caisseId) {
        return sessionRepository
                .findFirstByCaisseIdAndStatutOrderByDateOuvertureDesc(caisseId, StatutSessionCaisse.OUVERTE)
                .map(this::toResponse)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public List<SessionCaisseResponse> sessionsOuvertes() {
        return sessionRepository.findByStatutOrderByDateOuvertureDesc(StatutSessionCaisse.OUVERTE)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<SessionCaisseResponse> historique(Long caisseId, Pageable pageable) {
        return sessionRepository.findByCaisseIdOrderByDateOuvertureDesc(caisseId, pageable).map(this::toResponse);
    }

    // ------------------------------------------------------------------
    // Mouvements
    // ------------------------------------------------------------------

    /**
     * Point d'entrée unique de tout flux d'argent. Met à jour le solde de la
     * caisse et rattache le mouvement à la vacation ouverte si elle existe.
     *
     * Aucune session ouverte n'est exigée : les encaissements ne doivent jamais
     * échouer parce que la caissière a oublié d'ouvrir sa vacation. Le mouvement
     * est alors journalisé sans session et le contrôle se fera a posteriori.
     */
    @Transactional
    public MouvementCaisse enregistrer(Caisse caisse, SensMouvement sens, TypeMouvementCaisse type,
                                       BigDecimal montant, ModePaiement mode, String libelle,
                                       String reference, String origineType, Long origineId,
                                       Utilisateur agent) {
        return enregistrerInterne(caisse, null, sens, type, montant, mode, libelle, reference,
                origineType, origineId, agent, null);
    }

    /** Saisie manuelle d'un mouvement depuis l'écran de caisse. */
    @Transactional
    public MouvementCaisseResponse enregistrerManuel(MouvementCaisseRequest request) {
        Caisse caisse = resoudreCaisse(request.getCaisseId());
        SensMouvement sens = parseEnum(SensMouvement.class, request.getSens(), "sens du mouvement");
        TypeMouvementCaisse type = parseEnum(TypeMouvementCaisse.class, request.getType(), "type de mouvement");
        BigDecimal montant = request.getMontant();
        if (montant == null || montant.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Le montant du mouvement doit être supérieur à zéro");
        }
        ModePaiement mode = request.getMode() != null
                ? parseEnum(ModePaiement.class, request.getMode(), "mode de règlement")
                : ModePaiement.ESPECES;

        String libelle = request.getLibelle() != null && !request.getLibelle().isBlank()
                ? request.getLibelle() : libelleParDefaut(type);

        Utilisateur agent = getAgentConnecte();
        MouvementCaisse mvt = enregistrerInterne(caisse, null, sens, type, montant, mode, libelle,
                request.getReference(), "SAISIE_MANUELLE", null, agent, request.getNotes());

        auditService.log(agent, "MOUVEMENT_CAISSE", "MouvementCaisse", mvt.getId(),
                sens + " de " + montant + " FCFA sur " + caisse.getNom() + " — " + libelle);
        return toResponse(mvt);
    }

    private MouvementCaisse enregistrerInterne(Caisse caisse, SessionCaisse session, SensMouvement sens,
                                               TypeMouvementCaisse type, BigDecimal montant, ModePaiement mode,
                                               String libelle, String reference, String origineType,
                                               Long origineId, Utilisateur agent, String notes) {
        if (montant == null || montant.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Le montant d'un mouvement de caisse doit être supérieur à zéro");
        }

        SessionCaisse sessionCourante = session != null ? session
                : sessionRepository.findFirstByCaisseIdAndStatutOrderByDateOuvertureDesc(
                        caisse.getId(), StatutSessionCaisse.OUVERTE).orElse(null);

        // Le solde d'une caisse représente les espèces réellement détenues. Un
        // règlement par virement, carte ou mobile money est bien une recette (ou
        // une charge) de la résidence et figure au journal, mais il ne met ni ne
        // retire aucun billet du tiroir : il ne doit pas déplacer le solde, sinon
        // le comptage de fermeture ne tomberait jamais juste.
        ModePaiement modeEffectif = mode != null ? mode : ModePaiement.ESPECES;
        boolean toucheLeTiroir = modeEffectif == ModePaiement.ESPECES;

        BigDecimal nouveauSolde = caisse.getSolde();
        if (toucheLeTiroir) {
            nouveauSolde = sens == SensMouvement.ENTREE
                    ? caisse.getSolde().add(montant)
                    : caisse.getSolde().subtract(montant);

            // Un tiroir-caisse ne peut pas contenir moins que rien : refuser le
            // décaissement plutôt que d'afficher un solde négatif, qui ne
            // correspondrait à aucune réalité physique.
            if (sens == SensMouvement.SORTIE && nouveauSolde.compareTo(BigDecimal.ZERO) < 0) {
                throw new BusinessException("Espèces insuffisantes sur « " + caisse.getNom() + " » : "
                        + caisse.getSolde() + " FCFA en caisse, " + montant + " FCFA demandés. "
                        + "Approvisionnez la caisse ou réglez par un autre moyen.");
            }

            caisse.setSolde(nouveauSolde);
            caisseRepository.save(caisse);
        }

        MouvementCaisse mvt = MouvementCaisse.builder()
                .caisse(caisse)
                .session(sessionCourante)
                .sens(sens)
                .type(type)
                .montant(montant)
                .mode(modeEffectif)
                .libelle(libelle)
                .reference(reference)
                .origineType(origineType)
                .origineId(origineId)
                .soldeApres(nouveauSolde)
                .agent(agent)
                .dateMouvement(LocalDateTime.now())
                .notes(notes)
                .build();
        return mouvementRepository.save(mvt);
    }

    @Transactional(readOnly = true)
    public List<MouvementCaisseResponse> mouvementsSession(Long sessionId) {
        return mouvementRepository.findBySessionIdOrderByDateMouvementDesc(sessionId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<MouvementCaisseResponse> mouvementsCaisse(Long caisseId, Pageable pageable) {
        return mouvementRepository.findByCaisseIdOrderByDateMouvementDesc(caisseId, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public List<MouvementCaisseResponse> journal(LocalDateTime debut, LocalDateTime fin) {
        return mouvementRepository.findPeriode(debut, fin)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    public Caisse findCaisse(Long id) {
        return caisseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Caisse", id));
    }

    private TypeCaisse parseType(String value) {
        if (value == null || value.isBlank()) return TypeCaisse.RECEPTION;
        return parseEnum(TypeCaisse.class, value, "type de caisse");
    }

    static <E extends Enum<E>> E parseEnum(Class<E> type, String value, String libelle) {
        try {
            return Enum.valueOf(type, value);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new BusinessException("Valeur invalide pour le " + libelle + " : " + value);
        }
    }

    private String libelleParDefaut(TypeMouvementCaisse type) {
        return switch (type) {
            case FOND_DE_CAISSE -> "Fond de caisse";
            case ENCAISSEMENT_SEJOUR -> "Encaissement séjour";
            case ACOMPTE_RESERVATION -> "Acompte de réservation";
            case VENTE_RESTAURATION -> "Vente restauration";
            case RECETTE_EVENEMENT -> "Recette événement";
            case AUTRE_RECETTE -> "Autre recette";
            case REMBOURSEMENT -> "Remboursement client";
            case DEPENSE -> "Dépense";
            case ACHAT_PROVISION -> "Achat de provisions";
            case VERSEMENT_BANQUE -> "Versement bancaire";
            case TRANSFERT -> "Transfert entre caisses";
            case AJUSTEMENT -> "Ajustement de caisse";
        };
    }

    private Utilisateur getAgentConnecte() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return null;
        return utilisateurRepository.findByUsername(auth.getName()).orElse(null);
    }

    // ------------------------------------------------------------------
    // Mapping
    // ------------------------------------------------------------------

    public CaisseResponse toResponse(Caisse c) {
        SessionCaisse ouverte = sessionRepository
                .findFirstByCaisseIdAndStatutOrderByDateOuvertureDesc(c.getId(), StatutSessionCaisse.OUVERTE)
                .orElse(null);
        return CaisseResponse.builder()
                .id(c.getId())
                .nom(c.getNom())
                .type(c.getType().name())
                .solde(c.getSolde())
                .responsableId(c.getResponsable() != null ? c.getResponsable().getId() : null)
                .responsableNom(c.getResponsable() != null ? c.getResponsable().getNomComplet() : null)
                .description(c.getDescription())
                .actif(c.isActif())
                .sessionOuverteId(ouverte != null ? ouverte.getId() : null)
                .sessionCaissierNom(ouverte != null && ouverte.getCaissier() != null
                        ? ouverte.getCaissier().getNomComplet() : null)
                .build();
    }

    public SessionCaisseResponse toResponse(SessionCaisse s) {
        // Sur une vacation encore ouverte, les cumuls stockés ne sont pas à jour
        // (ils ne le deviennent qu'à la fermeture) : on les recalcule à la volée
        // pour que la caissière voie sa position réelle en cours de service.
        BigDecimal entrees = s.getStatut() == StatutSessionCaisse.OUVERTE
                ? mouvementRepository.sumBySessionAndSens(s.getId(), SensMouvement.ENTREE) : s.getTotalEntrees();
        BigDecimal sorties = s.getStatut() == StatutSessionCaisse.OUVERTE
                ? mouvementRepository.sumBySessionAndSens(s.getId(), SensMouvement.SORTIE) : s.getTotalSorties();
        BigDecimal theorique = s.getStatut() == StatutSessionCaisse.OUVERTE
                ? mouvementRepository.sumEspecesBySessionAndSens(s.getId(), SensMouvement.ENTREE)
                    .subtract(mouvementRepository.sumEspecesBySessionAndSens(s.getId(), SensMouvement.SORTIE))
                : s.getMontantTheorique();

        return SessionCaisseResponse.builder()
                .id(s.getId())
                .caisseId(s.getCaisse().getId())
                .caisseNom(s.getCaisse().getNom())
                .caissierId(s.getCaissier() != null ? s.getCaissier().getId() : null)
                .caissierNom(s.getCaissier() != null ? s.getCaissier().getNomComplet() : null)
                .dateOuverture(s.getDateOuverture())
                .fondCaisse(s.getFondCaisse())
                .dateFermeture(s.getDateFermeture())
                .totalEntrees(entrees)
                .totalSorties(sorties)
                .montantTheorique(theorique)
                .montantCompte(s.getMontantCompte())
                .ecart(s.getEcart())
                .statut(s.getStatut().name())
                .fermeParNom(s.getFermePar() != null ? s.getFermePar().getNomComplet() : null)
                .notes(s.getNotes())
                .nbMouvements(mouvementRepository.countBySessionId(s.getId()))
                .build();
    }

    public MouvementCaisseResponse toResponse(MouvementCaisse m) {
        return MouvementCaisseResponse.builder()
                .id(m.getId())
                .caisseId(m.getCaisse().getId())
                .caisseNom(m.getCaisse().getNom())
                .sessionId(m.getSession() != null ? m.getSession().getId() : null)
                .sens(m.getSens().name())
                .type(m.getType().name())
                .montant(m.getMontant())
                .mode(m.getMode().name())
                .libelle(m.getLibelle())
                .reference(m.getReference())
                .origineType(m.getOrigineType())
                .origineId(m.getOrigineId())
                .soldeApres(m.getSoldeApres())
                .agentNom(m.getAgent() != null ? m.getAgent().getNomComplet() : null)
                .dateMouvement(m.getDateMouvement())
                .notes(m.getNotes())
                .build();
    }
}
