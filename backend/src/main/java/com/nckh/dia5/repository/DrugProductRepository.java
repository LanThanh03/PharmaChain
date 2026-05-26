package com.nckh.dia5.repository;

import com.nckh.dia5.model.DrugProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface DrugProductRepository extends JpaRepository<DrugProduct, Long> {
    @Query("SELECT p FROM DrugProduct p WHERE p.manufacturerId = :manufacturerId")
    java.util.List<DrugProduct> findAllByManufacturerId(@Param("manufacturerId") Long manufacturerId);

    java.util.List<DrugProduct> findByManufacturerIdIsNull();

    java.util.Optional<DrugProduct> findByIdAndManufacturerId(Long id, Long manufacturerId);
    
    java.util.List<DrugProduct> findByName(String name);

    java.util.Optional<DrugProduct> findFirstByNameContainingIgnoreCase(String name);
}


