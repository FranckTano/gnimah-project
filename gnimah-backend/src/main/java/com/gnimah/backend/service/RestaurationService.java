package com.gnimah.backend.service;

import com.gnimah.backend.dto.restauration.*;
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
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Carte et additions du restaurant.
 *
 * Une addition peut être réglée en caisse ou portée au compte d'un séjour en
 * cours. Dans ce second cas rien n'est encaissé sur le moment : le montant
 * grossit le reste à payer de la chambre et sera perçu au départ du client,
 * avec le reste du séjour.
 */
@Service
@RequiredArgsConstructor
public class RestaurationService {

    private final ArticleRestaurationRepository articleRepository;
    private final CommandeRestaurantRepository commandeRepository;
    private final SejourRepository sejourRepository;
    private final ClientRepository clientRepository;
    private final EvenementRepository evenementRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final CaisseService caisseService;
    private final AuditService auditService;

    // ------------------------------------------------------------------
    // Carte
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<ArticleRestaurationResponse> carte(boolean disponiblesUniquement) {
        List<ArticleRestauration> articles = disponiblesUniquement
                ? articleRepository.findByDisponibleTrueOrderByCategorieAscNomAsc()
                : articleRepository.findAllByOrderByCategorieAscNomAsc();
        return articles.stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional
    public ArticleRestaurationResponse creerArticle(ArticleRestaurationRequest request) {
        if (request.getCode() != null && !request.getCode().isBlank()
                && articleRepository.existsByCode(request.getCode())) {
            throw new BusinessException("Le code « " + request.getCode() + " » est déjà utilisé");
        }
        ArticleRestauration a = ArticleRestauration.builder()
                .code(request.getCode() != null && !request.getCode().isBlank() ? request.getCode() : null)
                .nom(request.getNom())
                .categorie(CaisseService.parseEnum(
                        CategorieArticleResto.class, request.getCategorie(), "catégorie d'article"))
                .description(request.getDescription())
                .prixVente(request.getPrixVente())
                .coutRevient(request.getCoutRevient() != null ? request.getCoutRevient() : BigDecimal.ZERO)
                .disponible(request.getDisponible() == null || request.getDisponible())
                .photo(request.getPhoto())
                .build();
        return toResponse(articleRepository.save(a));
    }

    @Transactional
    public ArticleRestaurationResponse modifierArticle(Long id, ArticleRestaurationRequest request) {
        ArticleRestauration a = findArticle(id);
        a.setNom(request.getNom());
        a.setCategorie(CaisseService.parseEnum(
                CategorieArticleResto.class, request.getCategorie(), "catégorie d'article"));
        a.setDescription(request.getDescription());
        a.setPrixVente(request.getPrixVente());
        if (request.getCoutRevient() != null) a.setCoutRevient(request.getCoutRevient());
        if (request.getDisponible() != null) a.setDisponible(request.getDisponible());
        a.setPhoto(request.getPhoto());
        return toResponse(articleRepository.save(a));
    }

    @Transactional
    public ArticleRestaurationResponse basculerDisponibilite(Long id) {
        ArticleRestauration a = findArticle(id);
        a.setDisponible(!a.isDisponible());
        return toResponse(articleRepository.save(a));
    }

    @Transactional
    public void supprimerArticle(Long id) {
        // On désactive : les additions déjà émises référencent l'article et leur
        // détail doit rester consultable.
        ArticleRestauration a = findArticle(id);
        a.setDisponible(false);
        articleRepository.save(a);
    }

    // ------------------------------------------------------------------
    // Commandes
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public Page<CommandeResponse> findAll(Pageable pageable) {
        return commandeRepository.findAllByOrderByDateCommandeDesc(pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public List<CommandeResponse> findEnCours() {
        return commandeRepository.findEnCours().stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CommandeResponse getById(Long id) {
        return toResponse(findCommande(id));
    }

    @Transactional(readOnly = true)
    public List<CommandeResponse> findBySejour(Long sejourId) {
        return commandeRepository.findBySejourIdOrderByDateCommandeDesc(sejourId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional
    public CommandeResponse creer(CommandeRequest request) {
        if (request.getLignes() == null || request.getLignes().isEmpty()) {
            throw new BusinessException("Une commande doit comporter au moins une ligne");
        }

        Utilisateur agent = getAgentConnecte();
        TypeCommande type = request.getType() != null
                ? CaisseService.parseEnum(TypeCommande.class, request.getType(), "type de commande")
                : TypeCommande.SUR_PLACE;

        Sejour sejour = request.getSejourId() != null
                ? sejourRepository.findById(request.getSejourId())
                    .orElseThrow(() -> new ResourceNotFoundException("Séjour", request.getSejourId()))
                : null;

        boolean imputeSejour = Boolean.TRUE.equals(request.getImputeSejour());
        if (imputeSejour && sejour == null) {
            throw new BusinessException("Imputer l'addition à une chambre exige de désigner le séjour concerné");
        }
        if (imputeSejour && !"EN_COURS".equals(sejour.getStatut())) {
            throw new BusinessException("Le séjour " + sejour.getNumeroRecu()
                    + " est clôturé : son addition ne peut plus être augmentée");
        }

        CommandeRestaurant commande = CommandeRestaurant.builder()
                .numero(genererNumero())
                .type(type)
                .tableNumero(request.getTableNumero())
                .sejour(sejour)
                .client(request.getClientId() != null
                        ? clientRepository.findById(request.getClientId()).orElse(null)
                        : (sejour != null ? sejour.getClient() : null))
                .evenement(request.getEvenementId() != null
                        ? evenementRepository.findById(request.getEvenementId()).orElse(null) : null)
                .imputeSejour(imputeSejour)
                .statut(StatutCommande.OUVERTE)
                .agent(agent)
                .dateCommande(LocalDateTime.now())
                .notes(request.getNotes())
                .build();

        appliquerLignes(commande, request.getLignes());
        appliquerRemise(commande, request.getRemiseType(), request.getRemiseValeur(), request.getRemiseMotif());

        CommandeRestaurant saved = commandeRepository.save(commande);
        auditService.log(agent, "CREATION_COMMANDE", "CommandeRestaurant", saved.getId(),
                "Addition " + saved.getNumero() + " — " + saved.getMontantNet() + " FCFA");
        return toResponse(saved);
    }

    @Transactional
    public CommandeResponse modifier(Long id, CommandeRequest request) {
        CommandeRestaurant commande = findCommande(id);
        if (commande.getStatut() == StatutCommande.PAYEE || commande.getStatut() == StatutCommande.ANNULEE) {
            throw new BusinessException("Une addition réglée ou annulée ne peut plus être modifiée");
        }
        commande.getLignes().clear();
        appliquerLignes(commande, request.getLignes());
        appliquerRemise(commande, request.getRemiseType(), request.getRemiseValeur(), request.getRemiseMotif());
        if (request.getNotes() != null) commande.setNotes(request.getNotes());
        return toResponse(commandeRepository.save(commande));
    }

    @Transactional
    public CommandeResponse changerStatut(Long id, String statut) {
        CommandeRestaurant commande = findCommande(id);
        StatutCommande nouveau = CaisseService.parseEnum(StatutCommande.class, statut, "statut de commande");
        if (nouveau == StatutCommande.PAYEE) {
            throw new BusinessException("Le passage à « payée » se fait par l'encaissement de l'addition");
        }
        if (commande.getStatut() == StatutCommande.PAYEE) {
            throw new BusinessException("Une addition réglée ne change plus de statut");
        }
        commande.setStatut(nouveau);
        return toResponse(commandeRepository.save(commande));
    }

    /**
     * Encaisse l'addition, ou la porte au compte du séjour. Les deux voies
     * s'excluent : soit l'argent entre en caisse maintenant, soit il entrera au
     * départ du client avec le reste de sa note.
     */
    @Transactional
    public CommandeResponse encaisser(Long id, PaiementCommandeRequest request) {
        CommandeRestaurant commande = findCommande(id);
        if (commande.getStatut() == StatutCommande.PAYEE) {
            throw new BusinessException("Cette addition est déjà réglée");
        }
        if (commande.getStatut() == StatutCommande.ANNULEE) {
            throw new BusinessException("Une addition annulée ne peut pas être encaissée");
        }
        if (commande.getMontantNet().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Cette addition est vide");
        }

        Utilisateur agent = getAgentConnecte();
        boolean imputeSejour = Boolean.TRUE.equals(request.getImputeSejour()) || commande.isImputeSejour();

        if (imputeSejour) {
            Sejour sejour = commande.getSejour();
            if (sejour == null) {
                throw new BusinessException("Aucun séjour n'est rattaché à cette addition");
            }
            if (!"EN_COURS".equals(sejour.getStatut())) {
                throw new BusinessException("Le séjour " + sejour.getNumeroRecu() + " est clôturé");
            }
            sejour.setMontantExtras(nz(sejour.getMontantExtras()).add(commande.getMontantNet()));
            sejour.setMontantTotal(sejour.getMontantTotal().add(commande.getMontantNet()));
            sejour.setResteAPayer(sejour.getMontantTotal().subtract(sejour.getMontantPaye()));
            sejourRepository.save(sejour);

            commande.setImputeSejour(true);
            commande.setStatut(StatutCommande.PAYEE);
            commande.setDatePaiement(LocalDateTime.now());
            commande.setNotes(concat(commande.getNotes(),
                    "Portée au compte de la chambre " + sejour.getChambre().getNumero()));

            auditService.log(agent, "IMPUTATION_ADDITION", "CommandeRestaurant", commande.getId(),
                    commande.getMontantNet() + " FCFA portés au séjour " + sejour.getNumeroRecu());
        } else {
            ModePaiement mode = CaisseService.parseEnum(
                    ModePaiement.class, request.getMode(), "mode de paiement");
            Caisse caisse = caisseService.resoudreCaisse(caisseRestaurationId());

            caisseService.enregistrer(caisse, SensMouvement.ENTREE, TypeMouvementCaisse.VENTE_RESTAURATION,
                    commande.getMontantNet(), mode,
                    "Addition " + commande.getNumero()
                            + (commande.getTableNumero() != null ? " — table " + commande.getTableNumero() : ""),
                    request.getReferenceTransaction(), "COMMANDE_RESTAURANT", commande.getId(), agent);

            commande.setModePaiement(mode);
            commande.setMontantPaye(commande.getMontantNet());
            commande.setStatut(StatutCommande.PAYEE);
            commande.setDatePaiement(LocalDateTime.now());

            auditService.log(agent, "ENCAISSEMENT_ADDITION", "CommandeRestaurant", commande.getId(),
                    commande.getMontantNet() + " FCFA encaissés sur " + caisse.getNom());
        }

        if (request.getNotes() != null && !request.getNotes().isBlank()) {
            commande.setNotes(concat(commande.getNotes(), request.getNotes()));
        }
        return toResponse(commandeRepository.save(commande));
    }

    @Transactional
    public CommandeResponse annuler(Long id, String motif) {
        CommandeRestaurant commande = findCommande(id);
        if (commande.getStatut() == StatutCommande.PAYEE) {
            throw new BusinessException("Une addition réglée ne s'annule pas — passez par un remboursement en caisse");
        }
        commande.setStatut(StatutCommande.ANNULEE);
        if (motif != null && !motif.isBlank()) {
            commande.setNotes(concat(commande.getNotes(), "Annulation : " + motif));
        }
        return toResponse(commandeRepository.save(commande));
    }

    // ------------------------------------------------------------------
    // Calculs
    // ------------------------------------------------------------------

    private void appliquerLignes(CommandeRestaurant commande, List<LigneCommandeRequest> lignes) {
        BigDecimal brut = BigDecimal.ZERO;
        for (LigneCommandeRequest l : lignes) {
            if (l.getQuantite() == null || l.getQuantite() <= 0) {
                throw new BusinessException("La quantité d'une ligne doit être supérieure à zéro");
            }
            ArticleRestauration article = l.getArticleId() != null ? findArticle(l.getArticleId()) : null;

            String designation = l.getDesignation() != null && !l.getDesignation().isBlank()
                    ? l.getDesignation()
                    : (article != null ? article.getNom() : null);
            if (designation == null) {
                throw new BusinessException("Chaque ligne doit désigner un article de la carte ou porter un libellé");
            }

            BigDecimal prixUnitaire = l.getPrixUnitaire() != null ? l.getPrixUnitaire()
                    : (article != null ? article.getPrixVente() : null);
            if (prixUnitaire == null) {
                throw new BusinessException("Prix manquant pour « " + designation + " »");
            }

            BigDecimal montant = prixUnitaire.multiply(BigDecimal.valueOf(l.getQuantite()));
            brut = brut.add(montant);

            commande.getLignes().add(LigneCommande.builder()
                    .commande(commande)
                    .article(article)
                    .designation(designation)
                    .quantite(l.getQuantite())
                    .prixUnitaire(prixUnitaire)
                    .montant(montant)
                    .build());
        }
        commande.setMontantBrut(brut);
    }

    private void appliquerRemise(CommandeRestaurant commande, String remiseType,
                                 BigDecimal remiseValeur, String motif) {
        TypeRemise type = Remises.parse(remiseType);
        BigDecimal remise = Remises.calculer(commande.getMontantBrut(), type, remiseValeur);

        commande.setRemiseType(remise.compareTo(BigDecimal.ZERO) > 0 ? type : null);
        commande.setRemiseValeur(remiseValeur != null ? remiseValeur : BigDecimal.ZERO);
        commande.setRemiseMontant(remise);
        commande.setRemiseMotif(remise.compareTo(BigDecimal.ZERO) > 0 ? motif : null);
        commande.setMontantNet(commande.getMontantBrut().subtract(remise));
    }

    /** Les ventes du restaurant vont dans la caisse dédiée si elle existe. */
    private Long caisseRestaurationId() {
        return caisseService.findAll().stream()
                .filter(c -> TypeCaisse.RESTAURATION.name().equals(c.getType()))
                .map(com.gnimah.backend.dto.caisse.CaisseResponse::getId)
                .findFirst().orElse(null);
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    public ArticleRestauration findArticle(Long id) {
        return articleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Article de la carte", id));
    }

    public CommandeRestaurant findCommande(Long id) {
        return commandeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Commande", id));
    }

    private String genererNumero() {
        String prefix = "CMD-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "-";
        Integer max = commandeRepository.findMaxSequence(prefix);
        return prefix + String.format("%04d", (max != null ? max : 0) + 1);
    }

    private BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }

    private String concat(String existant, String ajout) {
        if (existant == null || existant.isBlank()) return ajout;
        return existant + "\n" + ajout;
    }

    private Utilisateur getAgentConnecte() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return null;
        return utilisateurRepository.findByUsername(auth.getName()).orElse(null);
    }

    // ------------------------------------------------------------------
    // Mapping
    // ------------------------------------------------------------------

    public ArticleRestaurationResponse toResponse(ArticleRestauration a) {
        return ArticleRestaurationResponse.builder()
                .id(a.getId())
                .code(a.getCode())
                .nom(a.getNom())
                .categorie(a.getCategorie().name())
                .description(a.getDescription())
                .prixVente(a.getPrixVente())
                .coutRevient(a.getCoutRevient())
                .marge(a.getPrixVente().subtract(nz(a.getCoutRevient())))
                .disponible(a.isDisponible())
                .photo(a.getPhoto())
                .build();
    }

    public CommandeResponse toResponse(CommandeRestaurant c) {
        List<LigneCommandeResponse> lignes = new ArrayList<>();
        for (LigneCommande l : c.getLignes()) {
            lignes.add(LigneCommandeResponse.builder()
                    .id(l.getId())
                    .articleId(l.getArticle() != null ? l.getArticle().getId() : null)
                    .designation(l.getDesignation())
                    .quantite(l.getQuantite())
                    .prixUnitaire(l.getPrixUnitaire())
                    .montant(l.getMontant())
                    .build());
        }

        return CommandeResponse.builder()
                .id(c.getId())
                .numero(c.getNumero())
                .type(c.getType().name())
                .tableNumero(c.getTableNumero())
                .sejourId(c.getSejour() != null ? c.getSejour().getId() : null)
                .sejourChambre(c.getSejour() != null ? c.getSejour().getChambre().getNumero() : null)
                .clientId(c.getClient() != null ? c.getClient().getId() : null)
                .clientNom(c.getClient() != null ? c.getClient().getNomComplet() : null)
                .evenementId(c.getEvenement() != null ? c.getEvenement().getId() : null)
                .evenementTitre(c.getEvenement() != null ? c.getEvenement().getIntitule() : null)
                .montantBrut(c.getMontantBrut())
                .remiseType(c.getRemiseType() != null ? c.getRemiseType().name() : null)
                .remiseValeur(c.getRemiseValeur())
                .remiseMontant(c.getRemiseMontant())
                .remiseMotif(c.getRemiseMotif())
                .montantNet(c.getMontantNet())
                .montantPaye(c.getMontantPaye())
                .statut(c.getStatut().name())
                .modePaiement(c.getModePaiement() != null ? c.getModePaiement().name() : null)
                .imputeSejour(c.isImputeSejour())
                .agentNom(c.getAgent() != null ? c.getAgent().getNomComplet() : null)
                .dateCommande(c.getDateCommande())
                .datePaiement(c.getDatePaiement())
                .notes(c.getNotes())
                .lignes(lignes)
                .build();
    }
}
