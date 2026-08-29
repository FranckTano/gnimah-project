package com.gnimah.backend.repository;

import com.gnimah.backend.entity.ArticleStock;
import com.gnimah.backend.entity.enums.CategorieStock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface ArticleStockRepository extends JpaRepository<ArticleStock, Long> {

    List<ArticleStock> findByActifTrueOrderByCategorieAscDesignationAsc();

    List<ArticleStock> findByCategorieAndActifTrueOrderByDesignationAsc(CategorieStock categorie);

    boolean existsByCode(String code);

    /** Articles à réapprovisionner : quantité retombée au niveau du seuil ou en dessous. */
    @Query("SELECT a FROM ArticleStock a WHERE a.actif = true AND a.quantite <= a.seuilAlerte " +
           "ORDER BY a.quantite ASC")
    List<ArticleStock> findEnAlerte();

    @Query("SELECT COALESCE(SUM(a.quantite * a.prixUnitaire), 0) FROM ArticleStock a WHERE a.actif = true")
    BigDecimal valeurTotaleStock();

    @Query("SELECT a.categorie, COUNT(a), COALESCE(SUM(a.quantite * a.prixUnitaire), 0) " +
           "FROM ArticleStock a WHERE a.actif = true GROUP BY a.categorie")
    List<Object[]> valeurParCategorie();
}
