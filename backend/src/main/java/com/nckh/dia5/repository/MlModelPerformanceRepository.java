package com.nckh.dia5.repository;

import com.nckh.dia5.model.MlModelPerformance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface MlModelPerformanceRepository extends JpaRepository<MlModelPerformance, Integer> {

    @Query("SELECT mmp FROM MlModelPerformance mmp WHERE mmp.modelName = :modelName ORDER BY mmp.evaluationDate DESC")
    List<MlModelPerformance> findByModelName(@Param("modelName") String modelName);

    @Query("SELECT mmp FROM MlModelPerformance mmp WHERE mmp.modelName = :modelName AND mmp.modelVersion = :version ORDER BY mmp.evaluationDate DESC")
    List<MlModelPerformance> findByModelNameAndVersion(@Param("modelName") String modelName,
            @Param("version") String version);

    Optional<MlModelPerformance> findTopByModelNameAndModelVersionOrderByEvaluationDateDesc(String modelName,
            String modelVersion);

    @Query("SELECT mmp FROM MlModelPerformance mmp WHERE mmp.evaluationDate = :date")
    List<MlModelPerformance> findByEvaluationDate(@Param("date") LocalDate date);

    @Query("SELECT mmp FROM MlModelPerformance mmp WHERE mmp.evaluationDate BETWEEN :startDate AND :endDate")
    List<MlModelPerformance> findByDateRange(@Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("SELECT DISTINCT mmp.modelName FROM MlModelPerformance mmp ORDER BY mmp.modelName")
    List<String> findAllModelNames();

    @Query("SELECT DISTINCT mmp.modelVersion FROM MlModelPerformance mmp WHERE mmp.modelName = :modelName ORDER BY mmp.modelVersion")
    List<String> findVersionsByModelName(@Param("modelName") String modelName);

    @Query("SELECT mmp FROM MlModelPerformance mmp ORDER BY mmp.evaluationDate DESC")
    List<MlModelPerformance> findLatestPerformances();
}
