package com.gnimah.backend.service;

import com.gnimah.backend.dto.incident.IncidentRequest;
import com.gnimah.backend.dto.incident.IncidentResponse;
import com.gnimah.backend.dto.incident.ResolutionIncidentRequest;
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
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Déclaration et suivi des objets ou matériels défectueux relevés en chambre.
 *
 * Le service d'entretien constate au moment du nettoyage ; il n'a ni à chiffrer
 * la réparation ni à décider qui paie. Il déclare, éventuellement avec une
 * estimation. La résolution — qui engage de l'argent — est un second geste,
 * réservé à l'encadrement : c'est là que l'incident devient soit une charge de
 * maintenance, soit un supplément facturé au client fautif.
 */
@Service
@RequiredArgsConstructor
public class IncidentService {

    private final IncidentChambreRepository incidentRepository;
    private final ChambreRepository chambreRepository;
    private final SejourRepository sejourRepository;
    private final ArticleStockRepository articleStockRepository;
    private final TacheEntretienRepository tacheRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final StockService stockService;
    private final DepenseService depenseService;
    private final NotificationService notificationService;
    private final AuditService auditService;

    // ------------------------------------------------------------------
    // Consultation
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<IncidentResponse> findAll() {
        return incidentRepository.findAllByOrderByDateIncidentDesc().stream()
                .map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<IncidentResponse> findAll(Pageable pageable) {
        return incidentRepository.findAllByOrderByDateIncidentDesc(pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public List<IncidentResponse> findOuverts() {
        return incidentRepository.findOuverts().stream()
                .map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<IncidentResponse> findByChambre(Long chambreId) {
        return incidentRepository.findByChambreIdOrderByDateIncidentDesc(chambreId).stream()
                .map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public IncidentResponse getById(Long id) {
        return toResponse(findById(id));
    }

    // ------------------------------------------------------------------
    // Déclaration
    // ------------------------------------------------------------------

    @Transactional
    public IncidentResponse declarer(IncidentRequest request) {
        Chambre chambre = chambreRepository.findById(request.getChambreId())
                .orElseThrow(() -> new ResourceNotFoundException("Chambre", request.getChambreId()));

        Sejour sejour = request.getSejourId() != null
                ? sejourRepository.findById(request.getSejourId()).orElse(null)
                : dernierSejour(chambre.getId());

        GraviteIncident gravite = request.getGravite() != null
                ? CaisseService.parseEnum(GraviteIncident.class, request.getGravite(), "gravité")
                : GraviteIncident.MINEUR;

        Utilisateur agent = getAgentConnecte();

        IncidentChambre incident = IncidentChambre.builder()
                .numero(genererNumero())
                .chambre(chambre)
                .sejour(sejour)
                .client(sejour != null ? sejour.getClient() : null)
                .type(CaisseService.parseEnum(TypeIncident.class, request.getType(), "type d'incident"))
                .designation(request.getDesignation())
                .articleStock(request.getArticleStockId() != null
                        ? articleStockRepository.findById(request.getArticleStockId()).orElse(null) : null)
                .quantite(request.getQuantite() != null && request.getQuantite() > 0 ? request.getQuantite() : 1)
                .gravite(gravite)
                .description(request.getDescription())
                .photo(request.getPhoto())
                .statut(StatutIncident.DECLARE)
                .coutEstime(request.getCoutEstime() != null ? request.getCoutEstime() : BigDecimal.ZERO)
                .imputeClient(Boolean.TRUE.equals(request.getImputeClient()))
                .declarePar(agent)
                .dateIncident(LocalDateTime.now())
                .notes(request.getNotes())
                .build();

        IncidentChambre saved = incidentRepository.save(incident);

        // Un incident sérieux ne se contente pas d'être noté : il ouvre une tâche
        // pour l'équipe technique, sans quoi la chambre resterait indisponible
        // sans que personne ne soit chargé d'agir.
        if (gravite != GraviteIncident.MINEUR) {
            TacheEntretien tache = TacheEntretien.builder()
                    .titre("Réparation — " + saved.getDesignation())
                    .chambre(chambre)
                    .typeTache(saved.getType() == TypeIncident.PANNE_EQUIPEMENT ? "MAINTENANCE" : "REPARATION")
                    .description("Incident " + saved.getNumero() + " (" + gravite + ") : "
                            + saved.getDesignation()
                            + (saved.getDescription() != null ? " — " + saved.getDescription() : ""))
                    .statut(StatutTache.A_FAIRE)
                    .priorite(gravite == GraviteIncident.MAJEUR ? 1 : 2)
                    .superviseur(agent)
                    .dateLimite(LocalDateTime.now().plusDays(gravite == GraviteIncident.MAJEUR ? 1 : 3))
                    .build();
            saved.setTache(tacheRepository.save(tache));
        }

        if (Boolean.TRUE.equals(request.getBloquerChambre()) || gravite == GraviteIncident.MAJEUR) {
            chambre.setEtat(EtatChambre.EN_MAINTENANCE);
            chambreRepository.save(chambre);
        }

        notificationService.creer("INCIDENT_CHAMBRE", "Matériel défectueux signalé",
                "Chambre " + chambre.getNumero() + " — " + saved.getDesignation()
                        + " (" + libelleGravite(gravite) + ")", "/entretien/incidents");

        auditService.log(agent, "DECLARATION_INCIDENT", "IncidentChambre", saved.getId(),
                "Chambre " + chambre.getNumero() + " — " + saved.getDesignation());

        return toResponse(incidentRepository.save(saved));
    }

    @Transactional
    public IncidentResponse prendreEnCharge(Long id) {
        IncidentChambre incident = findById(id);
        if (incident.getStatut() != StatutIncident.DECLARE) {
            throw new BusinessException("Seul un incident déclaré peut être pris en charge");
        }
        incident.setStatut(StatutIncident.EN_COURS);
        incident.setResoluPar(getAgentConnecte());
        return toResponse(incidentRepository.save(incident));
    }

    /**
     * Clôture de l'incident. Le coût réel devient soit une charge de maintenance
     * réglée par la résidence, soit un supplément sur la note du client — jamais
     * les deux, car ce serait facturer deux fois la même casse.
     */
    @Transactional
    public IncidentResponse resoudre(Long id, ResolutionIncidentRequest request) {
        IncidentChambre incident = findById(id);
        if (incident.getStatut() == StatutIncident.CLASSE_SANS_SUITE) {
            throw new BusinessException("Cet incident a été classé sans suite");
        }
        // Une seconde résolution engagerait une seconde fois le coût : nouvelle
        // dépense en caisse ou nouveau supplément sur la note du client.
        if (incident.getStatut() == StatutIncident.REPARE || incident.getStatut() == StatutIncident.REMPLACE
                || incident.getStatut() == StatutIncident.IMPUTE_CLIENT) {
            throw new BusinessException("Cet incident est déjà résolu");
        }

        StatutIncident statut = CaisseService.parseEnum(
                StatutIncident.class, request.getStatut(), "statut de résolution");
        if (statut == StatutIncident.DECLARE || statut == StatutIncident.EN_COURS) {
            throw new BusinessException("Issue de résolution invalide : choisissez réparé, remplacé ou classé sans suite");
        }
        Utilisateur agent = getAgentConnecte();
        BigDecimal cout = request.getCoutReel() != null ? request.getCoutReel() : incident.getCoutEstime();
        if (cout == null || cout.signum() < 0) {
            throw new BusinessException("Le coût réel ne peut pas être négatif");
        }
        boolean imputeClient = statut == StatutIncident.IMPUTE_CLIENT
                || (request.getImputeClient() != null ? request.getImputeClient() : incident.isImputeClient());

        incident.setCoutReel(cout);
        incident.setResoluPar(agent);
        incident.setDateResolution(LocalDateTime.now());
        if (request.getNotes() != null && !request.getNotes().isBlank()) {
            incident.setNotes((incident.getNotes() != null ? incident.getNotes() + "\n" : "") + request.getNotes());
        }

        if (statut == StatutIncident.CLASSE_SANS_SUITE) {
            // Classement sans suite : rien n'est engagé, le coût constaté est remis à zéro.
            incident.setCoutReel(BigDecimal.ZERO);
            incident.setImputeClient(false);
            incident.setStatut(statut);
        } else if (cout.compareTo(BigDecimal.ZERO) > 0 && imputeClient) {
            Sejour sejour = incident.getSejour();
            if (sejour == null) {
                throw new BusinessException("Aucun séjour n'est rattaché à cet incident : "
                        + "le coût ne peut pas être imputé à un client");
            }
            sejour.setMontantExtras(nz(sejour.getMontantExtras()).add(cout));
            sejour.setMontantTotal(sejour.getMontantTotal().add(cout));
            sejour.setResteAPayer(sejour.getMontantTotal().subtract(sejour.getMontantPaye()));
            sejourRepository.save(sejour);

            incident.setImputeClient(true);
            incident.setStatut(StatutIncident.IMPUTE_CLIENT);

            notificationService.creer("INCIDENT_CHAMBRE", "Dégât facturé au client",
                    cout + " FCFA ajoutés à la note de " + sejour.getClient().getNomComplet()
                            + " (chambre " + incident.getChambre().getNumero() + ")", "/sejours");
        } else {
            incident.setImputeClient(false);
            incident.setStatut(statut);
            if (cout.compareTo(BigDecimal.ZERO) > 0) {
                FamilleDepense famille = request.getFamilleDepense() != null
                        ? CaisseService.parseEnum(FamilleDepense.class, request.getFamilleDepense(), "poste de charge")
                        : FamilleDepense.MAINTENANCE;
                Depense depense = depenseService.creerEtRegler(
                        (statut == StatutIncident.REMPLACE ? "Remplacement " : "Réparation ")
                                + incident.getDesignation() + " — chambre " + incident.getChambre().getNumero(),
                        cout, famille, incident.getDesignation(), null, request.getCaisseId(),
                        ModePaiement.ESPECES, "Incident " + incident.getNumero(), agent);
                incident.setDepense(depense);
            }
        }

        // Un objet remplacé sort du stock : sans cela l'inventaire resterait
        // supérieur à ce qui existe réellement dans la réserve.
        if (statut == StatutIncident.REMPLACE && incident.getArticleStock() != null) {
            stockService.sortirPourIncident(incident.getArticleStock(),
                    BigDecimal.valueOf(incident.getQuantite()), incident.getChambre(),
                    "Remplacement suite à l'incident " + incident.getNumero(), agent);
        }

        if (incident.getTache() != null && incident.getTache().getStatut() != StatutTache.TERMINE) {
            incident.getTache().setStatut(StatutTache.TERMINE);
            incident.getTache().setDateFin(LocalDateTime.now());
            tacheRepository.save(incident.getTache());
        }

        if (Boolean.TRUE.equals(request.getLibererChambre())
                && incident.getChambre().getEtat() == EtatChambre.EN_MAINTENANCE) {
            incident.getChambre().setEtat(EtatChambre.A_NETTOYER);
            chambreRepository.save(incident.getChambre());
        }

        auditService.log(agent, "RESOLUTION_INCIDENT", "IncidentChambre", incident.getId(),
                incident.getNumero() + " — " + incident.getStatut() + ", coût " + incident.getCoutReel() + " FCFA");

        return toResponse(incidentRepository.save(incident));
    }

    @Transactional
    public void supprimer(Long id) {
        IncidentChambre incident = findById(id);
        if (incident.getDepense() != null) {
            throw new BusinessException("Cet incident a généré une dépense réglée : il ne peut pas être supprimé");
        }
        if (incident.isImputeClient()) {
            throw new BusinessException("Cet incident a été facturé au client : il ne peut pas être supprimé");
        }
        incidentRepository.delete(incident);
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    /**
     * Dernier séjour connu de la chambre : quand l'entretien signale un dégât
     * après un départ, c'est presque toujours l'occupant sortant qui est
     * concerné, et l'agent n'a pas le numéro de reçu sous les yeux.
     */
    private Sejour dernierSejour(Long chambreId) {
        return sejourRepository.findByChambreIdAndStatut(chambreId, "TERMINE").stream()
                .max(java.util.Comparator.comparing(Sejour::getDateEntree))
                .orElseGet(() -> sejourRepository.findByChambreIdAndStatut(chambreId, "EN_COURS")
                        .stream().findFirst().orElse(null));
    }

    public IncidentChambre findById(Long id) {
        return incidentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Incident", id));
    }

    private String genererNumero() {
        String prefix = "INC-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM")) + "-";
        Integer max = incidentRepository.findMaxSequence(prefix);
        return prefix + String.format("%04d", (max != null ? max : 0) + 1);
    }

    static String libelleGravite(GraviteIncident g) {
        return switch (g) {
            case MINEUR -> "mineur";
            case MOYEN -> "moyen";
            case MAJEUR -> "majeur";
        };
    }

    private BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }

    private Utilisateur getAgentConnecte() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return null;
        return utilisateurRepository.findByUsername(auth.getName()).orElse(null);
    }

    public IncidentResponse toResponse(IncidentChambre i) {
        return IncidentResponse.builder()
                .id(i.getId())
                .numero(i.getNumero())
                .chambreId(i.getChambre().getId())
                .chambreNumero(i.getChambre().getNumero())
                .sejourId(i.getSejour() != null ? i.getSejour().getId() : null)
                .sejourNumeroRecu(i.getSejour() != null ? i.getSejour().getNumeroRecu() : null)
                .clientId(i.getClient() != null ? i.getClient().getId() : null)
                .clientNom(i.getClient() != null ? i.getClient().getNomComplet() : null)
                .type(i.getType().name())
                .designation(i.getDesignation())
                .articleStockId(i.getArticleStock() != null ? i.getArticleStock().getId() : null)
                .articleDesignation(i.getArticleStock() != null ? i.getArticleStock().getDesignation() : null)
                .quantite(i.getQuantite())
                .gravite(i.getGravite().name())
                .description(i.getDescription())
                .photo(i.getPhoto())
                .statut(i.getStatut().name())
                .coutEstime(i.getCoutEstime())
                .coutReel(i.getCoutReel())
                .imputeClient(i.isImputeClient())
                .declareParNom(i.getDeclarePar() != null ? i.getDeclarePar().getNomComplet() : null)
                .resoluParNom(i.getResoluPar() != null ? i.getResoluPar().getNomComplet() : null)
                .tacheId(i.getTache() != null ? i.getTache().getId() : null)
                .depenseId(i.getDepense() != null ? i.getDepense().getId() : null)
                .dateIncident(i.getDateIncident())
                .dateResolution(i.getDateResolution())
                .notes(i.getNotes())
                .build();
    }
}
