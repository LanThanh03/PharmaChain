package com.nckh.dia5.repository;

import com.nckh.dia5.model.Province;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProvinceRepository extends JpaRepository<Province, Integer> {

    Optional<Province> findByName(String name);

    Optional<Province> findByCode(String code);

    @Query("SELECT p FROM Province p WHERE p.region = :region ORDER BY p.name")
    List<Province> findByRegion(@Param("region") Province.Region region);

    @Query("SELECT p FROM Province p WHERE p.climate = :climate ORDER BY p.name")
    List<Province> findByClimate(@Param("climate") Province.Climate climate);

    @Query("SELECT p FROM Province p ORDER BY p.name")
    List<Province> findAllOrderByName();
}
