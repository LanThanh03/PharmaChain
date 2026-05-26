package com.nckh.dia5.repository;

import com.nckh.dia5.model.Symptom;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SymptomRepository extends JpaRepository<Symptom, Integer> {

    @Query("SELECT s FROM Symptom s WHERE s.category = :category ORDER BY s.name")
    List<Symptom> findByCategory(@Param("category") String category);

    @Query("SELECT s FROM Symptom s WHERE s.severityScale = :scale ORDER BY s.name")
    List<Symptom> findBySeverityScale(@Param("scale") Symptom.SeverityScale scale);

    @Query("SELECT s FROM Symptom s WHERE s.name LIKE %:keyword% OR s.category LIKE %:keyword% ORDER BY s.name")
    Page<Symptom> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT DISTINCT s.category FROM Symptom s WHERE s.category IS NOT NULL ORDER BY s.category")
    List<String> findAllCategories();

    @Query("SELECT s FROM Symptom s WHERE s.name LIKE %:keyword% ORDER BY s.name")
    List<Symptom> findByNameContaining(@Param("keyword") String keyword);
}
