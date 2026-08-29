package com.gnimah.backend.service;

import com.gnimah.backend.dto.depense.DepenseRequest;
import com.gnimah.backend.dto.depense.DepenseResponse;
import com.gnimah.backend.entity.Caisse;
import com.gnimah.backend.entity.Depense;
import com.gnimah.backend.entity.Utilisateur;
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
import java.util.List;
import java.util.stream.Collectors;

/**
 * Gestion des charges de la résidence.
 *
 * Une dépense suit un circuit en trois temps — saisie, validation, règlement —
 * et seul le règlement fait sortir de l'argent d'une caisse. Cette séparation
 * évite qu'une simple saisie d'agent ne fasse plonger la trésorerie affichée,
 * tout en gardant trace des engagements pris.
 */
@Service
@RequiredArgsConstructor
public class DepenseService {

    private final DepenseRepository depenseRepository;
    private final FournisseurRepository fournisseurRepository;
    private final ChambreRepository chambreRepository;
    private final SejourRepository sejourRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final CaisseService caisseService;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public Page<DepenseResponse> findAll(Pageable pageable) {
        return depenseRepository.findAllByOrderByDateDepenseDescIdDesc(pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public List<DepenseResponse> findPeriode(LocalDate debut, LocalDate fin) {
        return depenseRepository.findByDateDepenseBetweenOrderByDateDepenseDesc(debut, fin)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<DepenseResponse> findEnAttente() {
        return depenseRepository.findByStatutOrderByDateDepenseDesc(StatutDepense.EN_ATTENTE)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public DepenseResponse getById(Long id) {
        return toResponse(findById(id));
    }

    @Transactional
    public DepenseResponse create(DepenseRequest request) {
        if (request.getMontant() == null || request.getMontant().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Le montant de la dépense doit être supérieur à zéro");
        }

        Utilisateur agent = getAgentConnecte();
        StatutDepense statut = request.getStatut() != null && !request.getStatut().isBlank()
                ? CaisseService.parseEnum(StatutDepense.class, request.getStatut(), "statut de dépense")
                : StatutDepense.EN_ATTENTE;

        Depense depense = Depense.builder()
                .libelle(request.getLibelle())
                .montant(request.getMontant())
                .famille(CaisseService.parseEnum(FamilleDepense.class, request.getFamille(), "poste de charge"))
                .categorie(request.getCategorie() != null && !request.getCategorie().isBlank()
                        ? request.getCategorie() : request.getFamille())
                .dateDepense(request.getDateDepense() != null ? request.getDateDepense() : LocalDate.now())
                .fournisseur(request.getFournisseurId() != null
                        ? fournisseurRepository.findById(request.getFournisseurId()).orElse(null) : null)
                .modeReglement(request.getModeReglement() != null
                        ? CaisseService.parseEnum(ModePaiement.class, request.getModeReglement(), "mode de règlement")
                        : ModePaiement.ESPECES)
                .referencePiece(request.getReferencePiece())
                .statut(StatutDepense.EN_ATTENTE)
                .chambre(request.getChambreId() != null
                        ? chambreRepository.findById(request.getChambreId()).orElse(null) : null)
                .sejour(request.getSejourId() != null
                        ? sejourRepository.findById(request.getSejourId()).orElse(null) : null)
                .agent(agent)
                .notes(request.getNotes())
                .build();

        Depense saved = depenseRepository.save(depense);
        auditService.log(agent, "CREATION_DEPENSE", "Depense", saved.getId(),
                saved.getLibelle() + " — " + saved.getMontant() + " FCFA (" + saved.getFamille() + ")");

        // Une dépense saisie directement comme réglée décaisse immédiatement :
        // c'est le cas courant des petits achats payés de la main à la main.
        if (statut == StatutDepense.PAYEE) {
            return payer(saved.getId(), request.getCaisseId());
        }
        if (statut == StatutDepense.VALIDEE) {
            return valider(saved.getId());
        }
        return toResponse(saved);
    }

    @Transactional
    public DepenseResponse update(Long id, DepenseRequest request) {
        Depense depense = findById(id);
        if (depense.getStatut() == StatutDepense.PAYEE) {
            throw new BusinessException("Une dépense déjà réglée ne peut plus être modifiée — "
                    + "annulez-la et saisissez-en une nouvelle.");
        }
        depense.setLibelle(request.getLibelle());
        depense.setMontant(request.getMontant());
        depense.setFamille(CaisseService.parseEnum(FamilleDepense.class, request.getFamille(), "poste de charge"));
        if (request.getCategorie() != null && !request.getCategorie().isBlank()) {
            depense.setCategorie(request.getCategorie());
        }
        if (request.getDateDepense() != null) depense.setDateDepense(request.getDateDepense());
        depense.setFournisseur(request.getFournisseurId() != null
                ? fournisseurRepository.findById(request.getFournisseurId()).orElse(null) : null);
        if (request.getModeReglement() != null) {
            depense.setModeReglement(CaisseService.parseEnum(
                    ModePaiement.class, request.getModeReglement(), "mode de règlement"));
        }
        depense.setReferencePiece(request.getReferencePiece());
        depense.setChambre(request.getChambreId() != null
                ? chambreRepository.findById(request.getChambreId()).orElse(null) : null);
        depense.setNotes(request.getNotes());
        return toResponse(depenseRepository.save(depense));
    }

    @Transactional
    public DepenseResponse valider(Long id) {
        Depense depense = findById(id);
        if (depense.getStatut() == StatutDepense.PAYEE) {
            throw new BusinessException("Cette dépense est déjà réglée");
        }
        Utilisateur agent = getAgentConnecte();
        depense.setStatut(StatutDepense.VALIDEE);
        depense.setValidePar(agent);
        depense.setDateValidation(LocalDateTime.now());
        auditService.log(agent, "VALIDATION_DEPENSE", "Depense", id,
                "Validation de « " + depense.getLibelle() + " » — " + depense.getMontant() + " FCFA");
        return toResponse(depenseRepository.save(depense));
    }

    @Transactional
    public DepenseResponse rejeter(Long id, String motif) {
        Depense depense = findById(id);
        if (depense.getStatut() == StatutDepense.PAYEE) {
            throw new BusinessException("Une dépense déjà réglée ne peut pas être rejetée");
        }
        Utilisateur agent = getAgentConnecte();
        depense.setStatut(StatutDepense.REJETEE);
        depense.setValidePar(agent);
        depense.setDateValidation(LocalDateTime.now());
        if (motif != null && !motif.isBlank()) {
            depense.setNotes((depense.getNotes() != null ? depense.getNotes() + "\n" : "") + "Rejet : " + motif);
        }
        auditService.log(agent, "REJET_DEPENSE", "Depense", id, "Rejet de « " + depense.getLibelle() + " »");
        return toResponse(depenseRepository.save(depense));
    }

    /**
     * Règle la dépense et sort l'argent de la caisse. Le décaissement n'a lieu
     * qu'ici : c'est le seul moment où la résidence se sépare réellement des
     * fonds.
     */
    @Transactional
    public DepenseResponse payer(Long id, Long caisseId) {
        Depense depense = findById(id);
        if (depense.getStatut() == StatutDepense.PAYEE) {
            throw new BusinessException("Cette dépense est déjà réglée");
        }
        if (depense.getStatut() == StatutDepense.REJETEE) {
            throw new BusinessException("Une dépense rejetée ne peut pas être réglée");
        }

        Utilisateur agent = getAgentConnecte();
        Caisse caisse = caisseService.resoudreCaisse(caisseId);

        TypeMouvementCaisse type = depense.getFamille() == FamilleDepense.PROVISIONS
                || depense.getFamille() == FamilleDepense.RESTAURATION
                ? TypeMouvementCaisse.ACHAT_PROVISION
                : TypeMouvementCaisse.DEPENSE;

        caisseService.enregistrer(caisse, SensMouvement.SORTIE, type, depense.getMontant(),
                depense.getModeReglement(),
                depense.getLibelle() + " (" + libelleFamille(depense.getFamille()) + ")",
                depense.getReferencePiece(), "DEPENSE", depense.getId(), agent);

        depense.setStatut(StatutDepense.PAYEE);
        depense.setCaisse(caisse);
        if (depense.getValidePar() == null) {
            depense.setValidePar(agent);
            depense.setDateValidation(LocalDateTime.now());
        }

        auditService.log(agent, "REGLEMENT_DEPENSE", "Depense", id,
                "Règlement de " + depense.getMontant() + " FCFA sur " + caisse.getNom());
        return toResponse(depenseRepository.save(depense));
    }

    @Transactional
    public void delete(Long id) {
        Depense depense = findById(id);
        if (depense.getStatut() == StatutDepense.PAYEE) {
            throw new BusinessException("Une dépense réglée ne peut pas être supprimée — "
                    + "elle est rattachée à un mouvement de caisse.");
        }
        depenseRepository.delete(depense);
    }

    /**
     * Crée une dépense déjà réglée, pour les modules qui génèrent une charge en
     * même temps que leur opération (achat de provisions, réparation d'un
     * incident). Le décaissement est immédiat.
     */
    @Transactional
    public Depense creerEtRegler(String libelle, BigDecimal montant, FamilleDepense famille,
                                 String categorie, Long fournisseurId, Long caisseId,
                                 ModePaiement mode, String notes, Utilisateur agent) {
        Depense depense = Depense.builder()
                .libelle(libelle)
                .montant(montant)
                .famille(famille)
                .categorie(categorie != null ? categorie : famille.name())
                .dateDepense(LocalDate.now())
                .fournisseur(fournisseurId != null ? fournisseurRepository.findById(fournisseurId).orElse(null) : null)
                .modeReglement(mode != null ? mode : ModePaiement.ESPECES)
                .statut(StatutDepense.PAYEE)
                .agent(agent)
                .validePar(agent)
                .dateValidation(LocalDateTime.now())
                .notes(notes)
                .build();

        Caisse caisse = caisseService.resoudreCaisse(caisseId);
        depense.setCaisse(caisse);
        Depense saved = depenseRepository.save(depense);

        caisseService.enregistrer(caisse, SensMouvement.SORTIE,
                famille == FamilleDepense.PROVISIONS || famille == FamilleDepense.RESTAURATION
                        ? TypeMouvementCaisse.ACHAT_PROVISION : TypeMouvementCaisse.DEPENSE,
                montant, depense.getModeReglement(), libelle, null, "DEPENSE", saved.getId(), agent);

        return saved;
    }

    public Depense findById(Long id) {
        return depenseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Dépense", id));
    }

    static String libelleFamille(FamilleDepense f) {
        return switch (f) {
            case RESTAURATION -> "Restauration";
            case PROVISIONS -> "Provisions";
            case MAINTENANCE -> "Maintenance";
            case ENTRETIEN -> "Entretien";
            case LINGERIE -> "Lingerie";
            case ENERGIE -> "Énergie & eau";
            case SALAIRES -> "Salaires";
            case CHARGES_LOCATIVES -> "Charges locatives";
            case MARKETING -> "Marketing";
            case TRANSPORT -> "Transport";
            case ADMINISTRATIF -> "Administratif";
            case TAXES -> "Taxes & impôts";
            case DIVERS -> "Divers";
        };
    }

    private Utilisateur getAgentConnecte() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return null;
        return utilisateurRepository.findByUsername(auth.getName()).orElse(null);
    }

    public DepenseResponse toResponse(Depense d) {
        return DepenseResponse.builder()
                .id(d.getId())
                .libelle(d.getLibelle())
                .montant(d.getMontant())
                .famille(d.getFamille().name())
                .categorie(d.getCategorie())
                .dateDepense(d.getDateDepense())
                .fournisseurId(d.getFournisseur() != null ? d.getFournisseur().getId() : null)
                .fournisseurNom(d.getFournisseur() != null ? d.getFournisseur().getNom() : null)
                .modeReglement(d.getModeReglement().name())
                .referencePiece(d.getReferencePiece())
                .statut(d.getStatut().name())
                .caisseId(d.getCaisse() != null ? d.getCaisse().getId() : null)
                .caisseNom(d.getCaisse() != null ? d.getCaisse().getNom() : null)
                .chambreId(d.getChambre() != null ? d.getChambre().getId() : null)
                .chambreNumero(d.getChambre() != null ? d.getChambre().getNumero() : null)
                .sejourId(d.getSejour() != null ? d.getSejour().getId() : null)
                .agentNom(d.getAgent() != null ? d.getAgent().getNomComplet() : null)
                .valideParNom(d.getValidePar() != null ? d.getValidePar().getNomComplet() : null)
                .dateValidation(d.getDateValidation())
                .notes(d.getNotes())
                .createdAt(d.getCreatedAt())
                .build();
    }
}
