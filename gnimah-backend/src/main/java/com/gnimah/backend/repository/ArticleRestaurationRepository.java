package com.gnimah.backend.repository;

import com.gnimah.backend.entity.ArticleRestauration;
import com.gnimah.backend.entity.enums.CategorieArticleResto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ArticleRestaurationRepository extends JpaRepository<ArticleRestauration, Long> {

    List<ArticleRestauration> findByDisponibleTrueOrderByCategorieAscNomAsc();

    List<ArticleRestauration> findAllByOrderByCategorieAscNomAsc();

    List<ArticleRestauration> findByCategorieOrderByNomAsc(CategorieArticleResto categorie);

    boolean existsByCode(String code);
}
