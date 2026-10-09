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
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.hibernate.Hibernate;
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

    @PersistenceContext
    private EntityManager entityManager;

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

    /**
     * Ouvre une vacation. Le fond de caisse est ce que la caissière compte dans
     * le tiroir en prenant son service.
     *
     * Ces espèces sont le plus souvent déjà au solde — c'est le fond laissé par
     * la vacation précédente. Les ré-enregistrer comme une entrée gonflerait le
     * solde du montant du fond à chaque ouverture : seul l'écart entre le
     * comptage et le solde théorique donne donc lieu à une écriture, un apport
     * s'il manque de la monnaie, une régularisation s'il manque des espèces.
     */
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
        if (fond.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("Le fond de caisse ne peut pas être négatif");
        }

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

        // Le fond reste une mise en place, pas une recette : l'apport est exclu
        // de l'assiette des versements (voir MouvementCaisseRepository.sumRecettePeriode).
        BigDecimal soldeTheorique = verrouiller(caisse).getSolde();
        BigDecimal apport = fond.subtract(soldeTheorique);
        if (apport.signum() > 0) {
            enregistrerInterne(caisse, saved, SensMouvement.ENTREE, TypeMouvementCaisse.FOND_DE_CAISSE,
                    apport, ModePaiement.ESPECES, "Fond de caisse — apport à l'ouverture",
                    null, ORIGINE_SESSION, saved.getId(), agent,
                    "Compté " + fond + " FCFA pour un solde théorique de " + soldeTheorique + " FCFA");
        } else if (apport.signum() < 0) {
            enregistrerInterne(caisse, saved, SensMouvement.SORTIE, TypeMouvementCaisse.AJUSTEMENT,
                    apport.negate(), ModePaiement.ESPECES, "Écart constaté à l'ouverture — espèces manquantes",
                    null, ORIGINE_SESSION, saved.getId(), agent,
                    "Compté " + fond + " FCFA pour un solde théorique de " + soldeTheorique + " FCFA");
        }

        auditService.log(agent, "OUVERTURE_CAISSE", "SessionCaisse", saved.getId(),
                "Ouverture de " + caisse.getNom() + " avec un fond de " + fond + " FCFA"
                        + (apport.signum() != 0 ? " (solde théorique " + soldeTheorique + " FCFA)" : ""));
        return toResponse(saved);
    }

    /**
     * Ferme la vacation sur le comptage de la caissière. L'écart est conservé
     * sur la session — c'est la pièce que la direction examine — et le solde
     * de la caisse est régularisé au montant compté : après fermeture, il doit
     * correspondre aux espèces réellement présentes dans le tiroir.
     */
    @Transactional
    public SessionCaisseResponse fermer(Long sessionId, FermetureCaisseRequest request) {
        SessionCaisse session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session de caisse", sessionId));
        if (session.getStatut() == StatutSessionCaisse.FERMEE) {
            throw new BusinessException("Cette session de caisse est déjà fermée");
        }
        BigDecimal compte = request.getMontantCompte();
        if (compte == null) {
            throw new BusinessException("Le montant compté en caisse est requis pour fermer la vacation");
        }
        if (compte.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("Le montant compté ne peut pas être négatif");
        }

        Position position = position(session);
        BigDecimal ecart = compte.subtract(position.theorique());
        Utilisateur agent = getAgentConnecte();

        Caisse caisse = verrouiller(session.getCaisse());
        BigDecimal regularisation = compte.subtract(caisse.getSolde());
        if (regularisation.signum() != 0) {
            enregistrerInterne(caisse, session,
                    regularisation.signum() > 0 ? SensMouvement.ENTREE : SensMouvement.SORTIE,
                    TypeMouvementCaisse.AJUSTEMENT, regularisation.abs(), ModePaiement.ESPECES,
                    "Régularisation à la fermeture — " + (regularisation.signum() > 0 ? "excédent" : "manquant"),
                    null, ORIGINE_SESSION, session.getId(), agent,
                    "Compté " + compte + " FCFA pour " + position.theorique() + " FCFA attendus");
        }

        session.setTotalEntrees(position.entrees());
        session.setTotalSorties(position.sorties());
        session.setMontantTheorique(position.theorique());
        session.setMontantCompte(compte);
        session.setEcart(ecart);
        session.setStatut(StatutSessionCaisse.FERMEE);
        session.setDateFermeture(LocalDateTime.now());
        session.setFermePar(agent);
        if (request.getNotes() != null && !request.getNotes().isBlank()) {
            session.setNotes(request.getNotes());
        }

        SessionCaisse saved = sessionRepository.save(session);
        auditService.log(agent, "FERMETURE_CAISSE", "SessionCaisse", saved.getId(),
                "Fermeture de " + session.getCaisse().getNom() + " — théorique " + position.theorique()
                        + ", compté " + compte + ", écart " + ecart + " FCFA");
        return toResponse(saved);
    }

    /** Écritures de régularisation du fond, exclues de l'activité de la vacation. */
    static final String ORIGINE_SESSION = "SESSION_CAISSE";

    /** Activité d'une vacation et espèces attendues dans le tiroir. */
    private record Position(BigDecimal entrees, BigDecimal sorties,
                            BigDecimal entreesEspeces, BigDecimal sortiesEspeces, BigDecimal theorique) {}

    private Position position(SessionCaisse s) {
        Long id = s.getId();
        BigDecimal entreesEspeces = mouvementRepository.sumActiviteEspecesBySessionAndSens(id, SensMouvement.ENTREE);
        BigDecimal sortiesEspeces = mouvementRepository.sumActiviteEspecesBySessionAndSens(id, SensMouvement.SORTIE);
        BigDecimal fond = s.getFondCaisse() != null ? s.getFondCaisse() : BigDecimal.ZERO;
        return new Position(
                mouvementRepository.sumActiviteBySessionAndSens(id, SensMouvement.ENTREE),
                mouvementRepository.sumActiviteBySessionAndSens(id, SensMouvement.SORTIE),
                entreesEspeces, sortiesEspeces,
                fond.add(entreesEspeces).subtract(sortiesEspeces));
    }

    /**
     * Pose un verrou d'écriture sur la ligne de la caisse et relit son solde.
     * Deux encaissements simultanés sur la même caisse (réception et bar, par
     * exemple) se succèdent ainsi au lieu de s'écraser : sans verrou, chacun
     * partirait du même solde et l'un des deux montants serait perdu.
     */
    private Caisse verrouiller(Caisse caisse) {
        if (entityManager != null) {
            Object entite = Hibernate.unproxy(caisse);
            if (entityManager.contains(entite)) {
                entityManager.refresh(entite, LockModeType.PESSIMISTIC_WRITE);
            }
        }
        return caisse;
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

        // Solde relu sous verrou : c'est lui que l'on va faire évoluer.
        verrouiller(caisse);

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
        // L'activité est toujours recalculée depuis le journal pour que la
        // caissière voie sa position réelle en cours de service. Une vacation
        // fermée garde en revanche le théorique figé au moment du comptage.
        Position p = position(s);
        boolean ouverte = s.getStatut() == StatutSessionCaisse.OUVERTE;
        BigDecimal entrees = ouverte ? p.entrees() : s.getTotalEntrees();
        BigDecimal sorties = ouverte ? p.sorties() : s.getTotalSorties();
        BigDecimal theorique = ouverte ? p.theorique() : s.getMontantTheorique();

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
                .entreesEspeces(p.entreesEspeces())
                .sortiesEspeces(p.sortiesEspeces())
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
