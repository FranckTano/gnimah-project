package com.gnimah.backend.service;

import com.gnimah.backend.dto.versement.*;
import com.gnimah.backend.entity.*;
import com.gnimah.backend.entity.enums.*;
import com.gnimah.backend.exception.BusinessException;
import com.gnimah.backend.exception.ResourceNotFoundException;
import com.gnimah.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Versement de la recette d'une caisse vers un compte bancaire ou un numéro
 * mobile money.
 *
 * Le cycle est volontairement en trois temps — préparation, dépôt, confirmation
 * — parce que ces gestes sont posés à des moments différents et parfois par des
 * personnes différentes : la caissière prépare et dépose, la direction confirme
 * au vu du reçu. Chaque étape laisse une action horodatée et signée, ce qui
 * permet de répondre à « qui a versé quoi, quand, et qui l'a vérifié ».
 */
@Service
@RequiredArgsConstructor
public class VersementService {

    private final VersementRepository versementRepository;
    private final ActionVersementRepository actionRepository;
    private final CompteFinancierRepository compteRepository;
    private final MouvementCaisseRepository mouvementRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final CaisseService caisseService;
    private final AuditService auditService;
    private final NotificationService notificationService;

    // ------------------------------------------------------------------
    // Consultation
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public Page<VersementResponse> findAll(Pageable pageable) {
        return versementRepository.findAll(pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public VersementResponse getById(Long id) {
        return toResponse(findById(id));
    }

    @Transactional(readOnly = true)
    public List<VersementResponse> findPeriode(LocalDate debut, LocalDate fin) {
        return versementRepository.findByDateVersementBetweenOrderByDateVersementDesc(debut, fin)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    /**
     * Assiette proposée à la caissière : ce qui a été encaissé sur la période et
     * qui n'a pas encore été reversé. Le fond de caisse en est exclu — il n'a
     * jamais été une recette.
     */
    @Transactional(readOnly = true)
    public RecetteAVerserResponse recetteAVerser(Long caisseId, LocalDate debut, LocalDate fin) {
        Caisse caisse = caisseService.resoudreCaisse(caisseId);
        LocalDateTime debutDT = debut.atStartOfDay();
        LocalDateTime finDT = fin.plusDays(1).atStartOfDay();

        BigDecimal recette = mouvementRepository.sumRecettePeriode(caisse.getId(), debutDT, finDT);
        BigDecimal dejaVerse = versementRepository.findChevauchements(caisse.getId(), debut, fin).stream()
                .map(Versement::getMontant)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return RecetteAVerserResponse.builder()
                .caisseId(caisse.getId())
                .caisseNom(caisse.getNom())
                .periodeDebut(debut)
                .periodeFin(fin)
                .recetteEncaissee(recette)
                .dejaVerse(dejaVerse)
                .resteAVerser(recette.subtract(dejaVerse).max(BigDecimal.ZERO))
                .soldeCaisse(caisse.getSolde())
                .build();
    }

    // ------------------------------------------------------------------
    // Cycle de vie
    // ------------------------------------------------------------------

    @Transactional
    public VersementResponse preparer(VersementRequest request) {
        Caisse caisse = caisseService.resoudreCaisse(request.getCaisseId());
        CompteFinancier compte = compteRepository.findById(request.getCompteId())
                .orElseThrow(() -> new ResourceNotFoundException("Compte de destination", request.getCompteId()));

        if (!compte.isActif()) {
            throw new BusinessException("Le compte « " + compte.getLibelle() + " » est désactivé");
        }
        if (compte.getIdentifiant() == null || compte.getIdentifiant().isBlank()) {
            throw new BusinessException("Le compte « " + compte.getLibelle() + " » n'a pas de "
                    + (compte.getType() == TypeCompteFinancier.MOBILE_MONEY ? "numéro de téléphone" : "numéro de compte")
                    + " — complétez-le avant de verser.");
        }

        PeriodiciteVersement periodicite = request.getPeriodicite() != null
                ? CaisseService.parseEnum(PeriodiciteVersement.class, request.getPeriodicite(), "périodicité")
                : PeriodiciteVersement.JOURNALIER;

        LocalDate dateVersement = request.getDateVersement() != null ? request.getDateVersement() : LocalDate.now();
        LocalDate[] periode = resoudrePeriode(periodicite, request.getPeriodeDebut(), request.getPeriodeFin(), dateVersement);

        BigDecimal montant = request.getMontant();
        if (montant == null || montant.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Le montant du versement doit être supérieur à zéro");
        }
        if (montant.compareTo(caisse.getSolde()) > 0) {
            throw new BusinessException("Le versement (" + montant + " FCFA) dépasse le solde de "
                    + caisse.getNom() + " (" + caisse.getSolde() + " FCFA)");
        }

        List<Versement> chevauchements = versementRepository.findChevauchements(caisse.getId(), periode[0], periode[1]);
        if (!chevauchements.isEmpty()) {
            BigDecimal dejaVerse = chevauchements.stream()
                    .map(Versement::getMontant).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal recette = mouvementRepository.sumRecettePeriode(caisse.getId(),
                    periode[0].atStartOfDay(), periode[1].plusDays(1).atStartOfDay());
            if (dejaVerse.add(montant).compareTo(recette) > 0) {
                throw new BusinessException("Un versement de " + dejaVerse + " FCFA couvre déjà cette période "
                        + "alors que la recette n'est que de " + recette + " FCFA — vérifiez la période.");
            }
        }

        Utilisateur agent = getAgentConnecte();
        Versement versement = Versement.builder()
                .numero(genererNumero())
                .caisse(caisse)
                .compte(compte)
                .periodicite(periodicite)
                .periodeDebut(periode[0])
                .periodeFin(periode[1])
                .montant(montant)
                .dateVersement(dateVersement)
                .heureVersement(request.getHeureVersement() != null ? request.getHeureVersement() : LocalTime.now())
                .mode(request.getMode() != null
                        ? CaisseService.parseEnum(ModePaiement.class, request.getMode(), "mode de versement")
                        : ModePaiement.ESPECES)
                .referenceTransaction(request.getReferenceTransaction())
                .statut(StatutVersement.PREPARE)
                .caissier(agent)
                .justificatifUrl(request.getJustificatifUrl())
                .notes(request.getNotes())
                .build();

        Versement saved = versementRepository.save(versement);
        journaliser(saved, TypeActionVersement.PREPARATION, montant, agent,
                "Versement préparé pour la période du " + periode[0] + " au " + periode[1]);

        auditService.log(agent, "PREPARATION_VERSEMENT", "Versement", saved.getId(),
                montant + " FCFA de " + caisse.getNom() + " vers " + compte.getLibelle());
        return toResponse(saved);
    }

    /**
     * Dépôt effectif : c'est ce geste qui sort l'argent de la caisse. Tant qu'il
     * n'a pas eu lieu, l'espèce est encore physiquement dans le tiroir.
     */
    @Transactional
    public VersementResponse deposer(Long id, ActionVersementRequest request) {
        Versement versement = findById(id);
        if (versement.getStatut() != StatutVersement.PREPARE) {
            throw new BusinessException("Seul un versement préparé peut être déposé (statut actuel : "
                    + versement.getStatut() + ")");
        }

        Utilisateur agent = getAgentConnecte();
        if (request != null && request.getReferenceTransaction() != null
                && !request.getReferenceTransaction().isBlank()) {
            versement.setReferenceTransaction(request.getReferenceTransaction());
        }

        // Le versement porte sur la recette en espèces (son assiette n'inclut
        // que le numéraire) : ce sont donc toujours des billets qui quittent le
        // tiroir, quel que soit le canal de dépôt — guichet bancaire ou agent
        // mobile money. Le canal reste consigné sur le versement lui-même.
        caisseService.enregistrer(versement.getCaisse(), SensMouvement.SORTIE,
                TypeMouvementCaisse.VERSEMENT_BANQUE, versement.getMontant(), ModePaiement.ESPECES,
                "Versement " + versement.getNumero() + " vers " + versement.getCompte().getLibelle(),
                versement.getReferenceTransaction(), "VERSEMENT", versement.getId(), agent);

        versement.setStatut(StatutVersement.EFFECTUE);
        versement.setDateVersement(LocalDate.now());
        versement.setHeureVersement(LocalTime.now());
        Versement saved = versementRepository.save(versement);

        journaliser(saved, TypeActionVersement.DEPOT, saved.getMontant(), agent,
                request != null && request.getCommentaire() != null ? request.getCommentaire()
                        : "Dépôt effectué sur " + saved.getCompte().getLibelle());

        notificationService.creer("VERSEMENT", "Versement à confirmer",
                saved.getMontant() + " FCFA déposés sur " + saved.getCompte().getLibelle()
                        + " — en attente de confirmation", "/caisse/versements");
        auditService.log(agent, "DEPOT_VERSEMENT", "Versement", saved.getId(),
                "Dépôt de " + saved.getMontant() + " FCFA");
        return toResponse(saved);
    }

    /** Confirmation par la direction, au vu du reçu de la banque ou du SMS de l'opérateur. */
    @Transactional
    public VersementResponse confirmer(Long id, ActionVersementRequest request) {
        Versement versement = findById(id);
        if (versement.getStatut() != StatutVersement.EFFECTUE) {
            throw new BusinessException("Seul un versement déposé peut être confirmé (statut actuel : "
                    + versement.getStatut() + ")");
        }

        Utilisateur agent = getAgentConnecte();
        versement.setStatut(StatutVersement.CONFIRME);
        versement.setValidePar(agent);
        versement.setDateValidation(LocalDateTime.now());
        if (request != null && request.getReferenceTransaction() != null
                && !request.getReferenceTransaction().isBlank()) {
            versement.setReferenceTransaction(request.getReferenceTransaction());
        }

        CompteFinancier compte = versement.getCompte();
        compte.setSolde(compte.getSolde().add(versement.getMontant()));
        compteRepository.save(compte);

        Versement saved = versementRepository.save(versement);
        journaliser(saved, TypeActionVersement.CONFIRMATION, saved.getMontant(), agent,
                request != null && request.getCommentaire() != null ? request.getCommentaire()
                        : "Versement confirmé au vu du justificatif");

        auditService.log(agent, "CONFIRMATION_VERSEMENT", "Versement", saved.getId(),
                "Confirmation de " + saved.getMontant() + " FCFA sur " + compte.getLibelle());
        return toResponse(saved);
    }

    /**
     * Annulation. Si le dépôt avait déjà eu lieu, l'argent revient en caisse par
     * un mouvement inverse plutôt que par la suppression du mouvement d'origine :
     * un journal de caisse ne se réécrit pas.
     */
    @Transactional
    public VersementResponse annuler(Long id, ActionVersementRequest request) {
        Versement versement = findById(id);
        if (versement.getStatut() == StatutVersement.ANNULE) {
            throw new BusinessException("Ce versement est déjà annulé");
        }

        Utilisateur agent = getAgentConnecte();
        boolean argentSorti = versement.getStatut() == StatutVersement.EFFECTUE
                || versement.getStatut() == StatutVersement.CONFIRME;

        if (argentSorti) {
            // Le mouvement inverse reprend le mode de la sortie d'origine : il
            // doit remettre au tiroir exactement ce que le dépôt en avait retiré.
            ModePaiement modeSortie = mouvementRepository.findByOrigineTypeAndOrigineId("VERSEMENT", versement.getId())
                    .stream()
                    .filter(m -> m.getSens() == SensMouvement.SORTIE)
                    .map(MouvementCaisse::getMode)
                    .findFirst()
                    .orElse(ModePaiement.ESPECES);
            caisseService.enregistrer(versement.getCaisse(), SensMouvement.ENTREE,
                    TypeMouvementCaisse.AJUSTEMENT, versement.getMontant(), modeSortie,
                    "Annulation du versement " + versement.getNumero(),
                    versement.getReferenceTransaction(), "VERSEMENT_ANNULE", versement.getId(), agent);
        }
        if (versement.getStatut() == StatutVersement.CONFIRME) {
            CompteFinancier compte = versement.getCompte();
            compte.setSolde(compte.getSolde().subtract(versement.getMontant()));
            compteRepository.save(compte);
        }

        versement.setStatut(StatutVersement.ANNULE);
        Versement saved = versementRepository.save(versement);
        journaliser(saved, TypeActionVersement.ANNULATION, saved.getMontant(), agent,
                request != null && request.getCommentaire() != null ? request.getCommentaire()
                        : "Versement annulé");

        auditService.log(agent, "ANNULATION_VERSEMENT", "Versement", saved.getId(),
                "Annulation de " + saved.getMontant() + " FCFA");
        return toResponse(saved);
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private void journaliser(Versement versement, TypeActionVersement action, BigDecimal montant,
                             Utilisateur agent, String commentaire) {
        ActionVersement a = ActionVersement.builder()
                .versement(versement)
                .action(action)
                .dateAction(LocalDateTime.now())
                .montant(montant)
                .agent(agent)
                .agentNom(agent != null ? agent.getNomComplet() : "Système")
                .commentaire(commentaire)
                .build();
        actionRepository.save(a);
    }

    /**
     * Périodicité et bornes. Si la caissière n'a rien précisé, la période se
     * déduit de la périodicité choisie et de la date de versement : la journée
     * écoulée, la semaine, ou le mois.
     */
    private LocalDate[] resoudrePeriode(PeriodiciteVersement periodicite, LocalDate debut,
                                        LocalDate fin, LocalDate dateVersement) {
        if (debut != null && fin != null) {
            if (fin.isBefore(debut)) {
                throw new BusinessException("La fin de période ne peut pas précéder son début");
            }
            return new LocalDate[]{debut, fin};
        }
        return switch (periodicite) {
            case JOURNALIER, PONCTUEL -> new LocalDate[]{dateVersement, dateVersement};
            case HEBDOMADAIRE -> new LocalDate[]{dateVersement.minusDays(6), dateVersement};
            case MENSUEL -> new LocalDate[]{dateVersement.withDayOfMonth(1), dateVersement};
        };
    }

    private String genererNumero() {
        String prefix = "VER-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM")) + "-";
        Integer max = versementRepository.findMaxSequence(prefix);
        return prefix + String.format("%04d", (max != null ? max : 0) + 1);
    }

    public Versement findById(Long id) {
        return versementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Versement", id));
    }

    private Utilisateur getAgentConnecte() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return null;
        return utilisateurRepository.findByUsername(auth.getName()).orElse(null);
    }

    public VersementResponse toResponse(Versement v) {
        List<ActionVersementResponse> actions = actionRepository
                .findByVersementIdOrderByDateActionAsc(v.getId()).stream()
                .map(a -> ActionVersementResponse.builder()
                        .id(a.getId())
                        .action(a.getAction().name())
                        .dateAction(a.getDateAction())
                        .montant(a.getMontant())
                        .agentNom(a.getAgentNom())
                        .commentaire(a.getCommentaire())
                        .build())
                .collect(Collectors.toList());

        return VersementResponse.builder()
                .id(v.getId())
                .numero(v.getNumero())
                .caisseId(v.getCaisse().getId())
                .caisseNom(v.getCaisse().getNom())
                .compteId(v.getCompte().getId())
                .compteLibelle(v.getCompte().getLibelle())
                .compteType(v.getCompte().getType().name())
                .compteIdentifiant(v.getCompte().getIdentifiant())
                .periodicite(v.getPeriodicite().name())
                .periodeDebut(v.getPeriodeDebut())
                .periodeFin(v.getPeriodeFin())
                .montant(v.getMontant())
                .dateVersement(v.getDateVersement())
                .heureVersement(v.getHeureVersement())
                .mode(v.getMode().name())
                .referenceTransaction(v.getReferenceTransaction())
                .statut(v.getStatut().name())
                .caissierNom(v.getCaissier() != null ? v.getCaissier().getNomComplet() : null)
                .valideParNom(v.getValidePar() != null ? v.getValidePar().getNomComplet() : null)
                .dateValidation(v.getDateValidation())
                .justificatifUrl(v.getJustificatifUrl())
                .notes(v.getNotes())
                .createdAt(v.getCreatedAt())
                .actions(actions)
                .build();
    }
}
