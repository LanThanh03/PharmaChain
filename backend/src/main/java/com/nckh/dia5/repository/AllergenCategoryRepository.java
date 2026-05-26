package com.nckh.dia5.repository;

import com.nckh.dia5.model.AllergenCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AllergenCategoryRepository extends JpaRepository<AllergenCategory, Integer> {

    @Query("SELECT ac FROM AllergenCategory ac WHERE ac.category = :category ORDER BY ac.name")
    List<AllergenCategory> findByCategory(@Param("category") String category);

    @Query("SELECT ac FROM AllergenCategory ac WHERE ac.name LIKE %:keyword% ORDER BY ac.name")
    List<AllergenCategory> searchByName(@Param("keyword") String keyword);

    @Query("SELECT DISTINCT ac.category FROM AllergenCategory ac ORDER BY ac.category")
    List<String> findAllCategories();
}
