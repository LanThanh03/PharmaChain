package com.nckh.dia5.repository;

import com.nckh.dia5.model.Medication;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MedicationRepository extends JpaRepository<Medication, Integer> {

    Optional<Medication> findByName(String name);

    Optional<Medication> findByGenericName(String genericName);

    @Query("SELECT m FROM Medication m WHERE m.drugClass = :drugClass ORDER BY m.name")
    List<Medication> findByDrugClass(@Param("drugClass") String drugClass);

    @Query("SELECT m FROM Medication m WHERE m.requiresPrescription = :requiresPrescription ORDER BY m.name")
    List<Medication> findByRequiresPrescription(@Param("requiresPrescription") Boolean requiresPrescription);

    @Query("SELECT m FROM Medication m WHERE m.isControlledSubstance = true ORDER BY m.name")
    List<Medication> findControlledSubstances();

    @Query("SELECT m FROM Medication m WHERE m.pregnancyCategory = :category ORDER BY m.name")
    List<Medication> findByPregnancyCategory(@Param("category") Medication.PregnancyCategory category);

    @Query("SELECT m FROM Medication m WHERE m.name LIKE %:keyword% OR m.genericName LIKE %:keyword% ORDER BY m.name")
    Page<Medication> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT DISTINCT m.drugClass FROM Medication m WHERE m.drugClass IS NOT NULL ORDER BY m.drugClass")
    List<String> findAllDrugClasses();
}
