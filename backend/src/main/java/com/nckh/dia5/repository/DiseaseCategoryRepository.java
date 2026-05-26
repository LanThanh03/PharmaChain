package com.nckh.dia5.repository;

import com.nckh.dia5.model.DiseaseCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DiseaseCategoryRepository extends JpaRepository<DiseaseCategory, Integer> {

    Optional<DiseaseCategory> findByName(String name);

    Optional<DiseaseCategory> findByIcd10Code(String icd10Code);

    @Query("SELECT dc FROM DiseaseCategory dc WHERE dc.specialty.id = :specialtyId ORDER BY dc.name")
    List<DiseaseCategory> findBySpecialtyId(@Param("specialtyId") Integer specialtyId);

    @Query("SELECT dc FROM DiseaseCategory dc WHERE dc.severityLevel = :severity ORDER BY dc.name")
    List<DiseaseCategory> findBySeverityLevel(@Param("severity") DiseaseCategory.SeverityLevel severity);

    @Query("SELECT dc FROM DiseaseCategory dc WHERE dc.isChronic = true ORDER BY dc.name")
    List<DiseaseCategory> findChronicDiseases();

    @Query("SELECT dc FROM DiseaseCategory dc WHERE dc.isContagious = true ORDER BY dc.name")
    List<DiseaseCategory> findContagiousDiseases();

    @Query("SELECT dc FROM DiseaseCategory dc WHERE dc.isHereditary = true ORDER BY dc.name")
    List<DiseaseCategory> findHereditaryDiseases();

    @Query("SELECT dc FROM DiseaseCategory dc WHERE dc.name LIKE %:keyword% OR dc.description LIKE %:keyword% ORDER BY dc.name")
    Page<DiseaseCategory> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);
}
