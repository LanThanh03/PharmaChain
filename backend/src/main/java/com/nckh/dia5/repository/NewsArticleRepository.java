package com.nckh.dia5.repository;

import com.nckh.dia5.model.NewsArticle;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NewsArticleRepository extends JpaRepository<NewsArticle, Integer> {

    @Query("SELECT na FROM NewsArticle na WHERE na.primaryCategory.id = :categoryId ORDER BY na.publicationDate DESC")
    Page<NewsArticle> findByCategoryId(@Param("categoryId") Integer categoryId, Pageable pageable);

    @Query("SELECT na FROM NewsArticle na WHERE na.targetAudience = :audience ORDER BY na.publicationDate DESC")
    Page<NewsArticle> findByTargetAudience(@Param("audience") NewsArticle.TargetAudience audience, Pageable pageable);

    @Query("SELECT na FROM NewsArticle na WHERE na.readingLevel = :level ORDER BY na.publicationDate DESC")
    Page<NewsArticle> findByReadingLevel(@Param("level") NewsArticle.ReadingLevel level, Pageable pageable);

    @Query("SELECT na FROM NewsArticle na WHERE na.factChecked = true ORDER BY na.publicationDate DESC")
    Page<NewsArticle> findFactCheckedArticles(Pageable pageable);

    @Query("SELECT na FROM NewsArticle na WHERE na.publicationDate >= :since ORDER BY na.publicationDate DESC")
    Page<NewsArticle> findRecentArticles(@Param("since") LocalDateTime since, Pageable pageable);

    @Query("SELECT na FROM NewsArticle na WHERE na.title LIKE %:keyword% OR na.summary LIKE %:keyword% OR na.content LIKE %:keyword% ORDER BY na.publicationDate DESC")
    Page<NewsArticle> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT na FROM NewsArticle na ORDER BY na.viewCount DESC")
    Page<NewsArticle> findMostViewed(Pageable pageable);

    @Query("SELECT na FROM NewsArticle na ORDER BY na.shareCount DESC")
    Page<NewsArticle> findMostShared(Pageable pageable);

    @Query("SELECT na FROM NewsArticle na WHERE na.medicalAccuracyScore >= :minScore ORDER BY na.medicalAccuracyScore DESC")
    List<NewsArticle> findHighQualityArticles(@Param("minScore") Double minScore);
}
