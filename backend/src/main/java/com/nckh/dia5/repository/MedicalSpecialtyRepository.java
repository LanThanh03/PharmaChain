package com.nckh.dia5.repository;

import com.nckh.dia5.model.MedicalSpecialty;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MedicalSpecialtyRepository extends JpaRepository<MedicalSpecialty, Integer> {

    Optional<MedicalSpecialty> findByName(String name);

    @Query("SELECT ms FROM MedicalSpecialty ms WHERE ms.parentSpecialty IS NULL ORDER BY ms.name")
    List<MedicalSpecialty> findRootSpecialties();

    @Query("SELECT ms FROM MedicalSpecialty ms WHERE ms.parentSpecialty.id = :parentId ORDER BY ms.name")
    List<MedicalSpecialty> findByParentSpecialtyId(@Param("parentId") Integer parentId);

    @Query("SELECT ms FROM MedicalSpecialty ms WHERE ms.name LIKE %:keyword% OR ms.description LIKE %:keyword%")
    List<MedicalSpecialty> searchByKeyword(@Param("keyword") String keyword);
}
