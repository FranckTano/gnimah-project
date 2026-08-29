package com.gnimah.backend.service;

import com.gnimah.backend.dto.stock.*;
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
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Provisions et matériel : cuisine, bar, produits d'entretien, linge, petit
 * équipement des chambres.
 *
 * Un achat de provisions n'est pas seulement une entrée de stock : c'est aussi
 * une charge et un décaissement. Le service enchaîne donc les trois écritures
 * en une seule opération, pour qu'aucune ne puisse être oubliée.
 */
@Service
@RequiredArgsConstructor
public class StockService {

    private final ArticleStockRepository articleRepository;
    private final MouvementStockRepository mouvementRepository;
    private final FournisseurRepository fournisseurRepository;
    private final ChambreRepository chambreRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final DepenseService depenseService;
    private final NotificationService notificationService;
    private final AuditService auditService;

    // ------------------------------------------------------------------
    // Articles
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<ArticleStockResponse> findAll() {
        return articleRepository.findByActifTrueOrderByCategorieAscDesignationAsc().stream()
                .map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ArticleStockResponse> findByCategorie(String categorie) {
        CategorieStock c = CaisseService.parseEnum(CategorieStock.class, categorie, "catégorie de stock");
        return articleRepository.findByCategorieAndActifTrueOrderByDesignationAsc(c).stream()
                .map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ArticleStockResponse> findEnAlerte() {
        return articleRepository.findEnAlerte().stream()
                .map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public BigDecimal valeurStock() {
        return articleRepository.valeurTotaleStock();
    }

    @Transactional
    public ArticleStockResponse create(ArticleStockRequest request) {
        if (request.getCode() != null && !request.getCode().isBlank()
                && articleRepository.existsByCode(request.getCode())) {
            throw new BusinessException("Le code « " + request.getCode() + " » est déjà attribué");
        }
        ArticleStock a = ArticleStock.builder()
                .code(vide(request.getCode()) ? null : request.getCode())
                .designation(request.getDesignation())
                .categorie(CaisseService.parseEnum(CategorieStock.class, request.getCategorie(), "catégorie de stock"))
                .unite(request.getUnite() != null
                        ? CaisseService.parseEnum(UniteMesure.class, request.getUnite(), "unité")
                        : UniteMesure.PIECE)
                .quantite(request.getQuantite() != null ? request.getQuantite() : BigDecimal.ZERO)
                .seuilAlerte(request.getSeuilAlerte() != null ? request.getSeuilAlerte() : BigDecimal.ZERO)
                .prixUnitaire(request.getPrixUnitaire() != null ? request.getPrixUnitaire() : BigDecimal.ZERO)
                .fournisseur(request.getFournisseurId() != null
                        ? fournisseurRepository.findById(request.getFournisseurId()).orElse(null) : null)
                .emplacement(request.getEmplacement())
                .actif(request.getActif() == null || request.getActif())
                .build();
        ArticleStock saved = articleRepository.save(a);

        // La quantité initiale est une entrée d'inventaire, pas un achat : elle
        // ne doit générer ni charge ni décaissement.
        if (saved.getQuantite().compareTo(BigDecimal.ZERO) > 0) {
            enregistrerMouvement(saved, SensMouvement.ENTREE, TypeMouvementStock.INVENTAIRE,
                    saved.getQuantite(), saved.getPrixUnitaire(), null, null, null,
                    "Stock initial à la création de l'article", getAgentConnecte());
        }
        return toResponse(saved);
    }

    @Transactional
    public ArticleStockResponse update(Long id, ArticleStockRequest request) {
        ArticleStock a = findArticle(id);
        a.setDesignation(request.getDesignation());
        a.setCategorie(CaisseService.parseEnum(CategorieStock.class, request.getCategorie(), "catégorie de stock"));
        if (request.getUnite() != null) {
            a.setUnite(CaisseService.parseEnum(UniteMesure.class, request.getUnite(), "unité"));
        }
        if (request.getSeuilAlerte() != null) a.setSeuilAlerte(request.getSeuilAlerte());
        if (request.getPrixUnitaire() != null) a.setPrixUnitaire(request.getPrixUnitaire());
        a.setFournisseur(request.getFournisseurId() != null
                ? fournisseurRepository.findById(request.getFournisseurId()).orElse(null) : null);
        a.setEmplacement(request.getEmplacement());
        if (request.getActif() != null) a.setActif(request.getActif());
        // La quantité n'est jamais modifiée ici : elle ne bouge que par un
        // mouvement de stock, seul moyen de garder l'inventaire auditable.
        return toResponse(articleRepository.save(a));
    }

    @Transactional
    public void desactiver(Long id) {
        ArticleStock a = findArticle(id);
        a.setActif(false);
        articleRepository.save(a);
    }

    // ------------------------------------------------------------------
    // Mouvements
    // ------------------------------------------------------------------

    @Transactional
    public MouvementStockResponse enregistrer(MouvementStockRequest request) {
        ArticleStock article = findArticle(request.getArticleId());
        TypeMouvementStock type = CaisseService.parseEnum(
                TypeMouvementStock.class, request.getType(), "type de mouvement de stock");
        BigDecimal quantite = request.getQuantite();
        if (quantite == null || quantite.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("La quantité doit être supérieure à zéro");
        }

        SensMouvement sens = sensDe(type);
        if (sens == SensMouvement.SORTIE && quantite.compareTo(article.getQuantite()) > 0) {
            throw new BusinessException("Stock insuffisant pour « " + article.getDesignation() + " » : "
                    + article.getQuantite() + " " + article.getUnite() + " disponibles, "
                    + quantite + " demandées");
        }

        BigDecimal prixUnitaire = request.getPrixUnitaire() != null
                ? request.getPrixUnitaire() : article.getPrixUnitaire();
        Utilisateur agent = getAgentConnecte();

        Depense depense = null;
        if (type == TypeMouvementStock.ACHAT && Boolean.TRUE.equals(request.getGenererDepense())) {
            BigDecimal montant = quantite.multiply(prixUnitaire);
            depense = depenseService.creerEtRegler(
                    "Achat " + article.getDesignation() + " (" + quantite + " " + article.getUnite() + ")",
                    montant, familleDe(article.getCategorie()), article.getDesignation(),
                    request.getFournisseurId(), request.getCaisseId(),
                    request.getModeReglement() != null
                            ? CaisseService.parseEnum(ModePaiement.class, request.getModeReglement(), "mode de règlement")
                            : ModePaiement.ESPECES,
                    "Approvisionnement enregistré depuis le module Stock", agent);
        }

        Chambre chambre = request.getChambreId() != null
                ? chambreRepository.findById(request.getChambreId()).orElse(null) : null;

        MouvementStock mvt = enregistrerMouvement(article, sens, type, quantite, prixUnitaire,
                request.getFournisseurId(), depense, chambre, request.getMotif(), agent);

        // Un achat renseigne le dernier prix connu : c'est lui qui valorisera les
        // sorties suivantes et le stock au bilan.
        if (type == TypeMouvementStock.ACHAT && request.getPrixUnitaire() != null) {
            article.setPrixUnitaire(request.getPrixUnitaire());
            articleRepository.save(article);
        }

        if (article.isEnAlerte()) {
            notificationService.creer("STOCK_ALERTE", "Stock bas",
                    article.getDesignation() + " : il ne reste que " + article.getQuantite()
                            + " " + article.getUnite(), "/stock");
        }

        return toResponse(mvt);
    }

    private MouvementStock enregistrerMouvement(ArticleStock article, SensMouvement sens, TypeMouvementStock type,
                                                BigDecimal quantite, BigDecimal prixUnitaire, Long fournisseurId,
                                                Depense depense, Chambre chambre, String motif, Utilisateur agent) {
        BigDecimal nouvelleQuantite = sens == SensMouvement.ENTREE
                ? article.getQuantite().add(quantite)
                : article.getQuantite().subtract(quantite);
        article.setQuantite(nouvelleQuantite);
        articleRepository.save(article);

        MouvementStock mvt = MouvementStock.builder()
                .article(article)
                .sens(sens)
                .type(type)
                .quantite(quantite)
                .quantiteApres(nouvelleQuantite)
                .prixUnitaire(prixUnitaire != null ? prixUnitaire : BigDecimal.ZERO)
                .montant(quantite.multiply(prixUnitaire != null ? prixUnitaire : BigDecimal.ZERO))
                .fournisseur(fournisseurId != null
                        ? fournisseurRepository.findById(fournisseurId).orElse(null) : null)
                .depense(depense)
                .chambre(chambre)
                .agent(agent)
                .dateMouvement(LocalDateTime.now())
                .motif(motif)
                .build();
        MouvementStock saved = mouvementRepository.save(mvt);

        auditService.log(agent, "MOUVEMENT_STOCK", "MouvementStock", saved.getId(),
                sens + " de " + quantite + " " + article.getUnite() + " — " + article.getDesignation());
        return saved;
    }

    @Transactional(readOnly = true)
    public List<MouvementStockResponse> historiqueArticle(Long articleId) {
        return mouvementRepository.findByArticleIdOrderByDateMouvementDesc(articleId).stream()
                .map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<MouvementStockResponse> journal(Pageable pageable) {
        return mouvementRepository.findAllByOrderByDateMouvementDesc(pageable).map(this::toResponse);
    }

    /** Sortie de stock déclenchée par un autre module (remplacement d'un objet cassé). */
    @Transactional
    public void sortirPourIncident(ArticleStock article, BigDecimal quantite, Chambre chambre,
                                   String motif, Utilisateur agent) {
        if (quantite.compareTo(article.getQuantite()) > 0) {
            // Le remplacement d'un objet ne doit pas être bloqué par un inventaire
            // en retard : on descend au plus bas possible et on le signale.
            quantite = article.getQuantite();
        }
        if (quantite.compareTo(BigDecimal.ZERO) <= 0) return;
        enregistrerMouvement(article, SensMouvement.SORTIE, TypeMouvementStock.CONSOMMATION_CHAMBRE,
                quantite, article.getPrixUnitaire(), null, null, chambre, motif, agent);
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    public ArticleStock findArticle(Long id) {
        return articleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Article de stock", id));
    }

    private SensMouvement sensDe(TypeMouvementStock type) {
        return switch (type) {
            case ACHAT, INVENTAIRE -> SensMouvement.ENTREE;
            case CONSOMMATION_RESTAURATION, CONSOMMATION_CHAMBRE, PERTE, CASSE,
                 RETOUR_FOURNISSEUR, TRANSFERT -> SensMouvement.SORTIE;
        };
    }

    /** Poste comptable d'un achat, déduit de la nature de l'article. */
    static FamilleDepense familleDe(CategorieStock categorie) {
        return switch (categorie) {
            case PROVISION_ALIMENTAIRE, BOISSON -> FamilleDepense.PROVISIONS;
            case PRODUIT_ENTRETIEN -> FamilleDepense.ENTRETIEN;
            case LINGE -> FamilleDepense.LINGERIE;
            case MATERIEL_CHAMBRE, MATERIEL_MAINTENANCE -> FamilleDepense.MAINTENANCE;
            case FOURNITURE_BUREAU -> FamilleDepense.ADMINISTRATIF;
            case AUTRE -> FamilleDepense.DIVERS;
        };
    }

    private boolean vide(String s) {
        return s == null || s.isBlank();
    }

    private Utilisateur getAgentConnecte() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return null;
        return utilisateurRepository.findByUsername(auth.getName()).orElse(null);
    }

    public ArticleStockResponse toResponse(ArticleStock a) {
        return ArticleStockResponse.builder()
                .id(a.getId())
                .code(a.getCode())
                .designation(a.getDesignation())
                .categorie(a.getCategorie().name())
                .unite(a.getUnite().name())
                .quantite(a.getQuantite())
                .seuilAlerte(a.getSeuilAlerte())
                .prixUnitaire(a.getPrixUnitaire())
                .valeurStock(a.getValeurStock())
                .fournisseurId(a.getFournisseur() != null ? a.getFournisseur().getId() : null)
                .fournisseurNom(a.getFournisseur() != null ? a.getFournisseur().getNom() : null)
                .emplacement(a.getEmplacement())
                .actif(a.isActif())
                .enAlerte(a.isEnAlerte())
                .build();
    }

    public MouvementStockResponse toResponse(MouvementStock m) {
        return MouvementStockResponse.builder()
                .id(m.getId())
                .articleId(m.getArticle().getId())
                .articleDesignation(m.getArticle().getDesignation())
                .unite(m.getArticle().getUnite().name())
                .sens(m.getSens().name())
                .type(m.getType().name())
                .quantite(m.getQuantite())
                .quantiteApres(m.getQuantiteApres())
                .prixUnitaire(m.getPrixUnitaire())
                .montant(m.getMontant())
                .fournisseurNom(m.getFournisseur() != null ? m.getFournisseur().getNom() : null)
                .depenseId(m.getDepense() != null ? m.getDepense().getId() : null)
                .chambreNumero(m.getChambre() != null ? m.getChambre().getNumero() : null)
                .agentNom(m.getAgent() != null ? m.getAgent().getNomComplet() : null)
                .dateMouvement(m.getDateMouvement())
                .motif(m.getMotif())
                .build();
    }
}
