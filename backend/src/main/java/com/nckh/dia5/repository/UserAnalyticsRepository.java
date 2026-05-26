package com.nckh.dia5.repository;

import com.nckh.dia5.model.UserAnalytics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserAnalyticsRepository extends JpaRepository<UserAnalytics, Integer> {

    Optional<UserAnalytics> findByUserIdAndSessionDate(String userId, LocalDate sessionDate);

    @Query("SELECT ua FROM UserAnalytics ua WHERE ua.user.id = :userId ORDER BY ua.sessionDate DESC")
    List<UserAnalytics> findByUserId(@Param("userId") String userId);

    @Query("SELECT ua FROM UserAnalytics ua WHERE ua.sessionDate BETWEEN :startDate AND :endDate")
    List<UserAnalytics> findByDateRange(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("SELECT ua FROM UserAnalytics ua WHERE ua.user.id = :userId AND ua.sessionDate BETWEEN :startDate AND :endDate")
    List<UserAnalytics> findByUserIdAndDateRange(@Param("userId") String userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("SELECT SUM(ua.totalSessions) FROM UserAnalytics ua WHERE ua.sessionDate = :date")
    Long getTotalSessionsForDate(@Param("date") LocalDate date);

    @Query("SELECT SUM(ua.totalTimeMinutes) FROM UserAnalytics ua WHERE ua.user.id = :userId")
    Long getTotalTimeByUserId(@Param("userId") String userId);

    @Query("SELECT AVG(ua.averageSatisfaction) FROM UserAnalytics ua WHERE ua.averageSatisfaction IS NOT NULL")
    Double getOverallAverageSatisfaction();

    @Query("SELECT ua FROM UserAnalytics ua WHERE ua.sessionDate >= :since ORDER BY ua.totalSessions DESC")
    List<UserAnalytics> findMostActiveUsersSince(@Param("since") LocalDate since);
}
