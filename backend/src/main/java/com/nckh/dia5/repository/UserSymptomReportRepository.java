package com.nckh.dia5.repository;

import com.nckh.dia5.model.User;
import com.nckh.dia5.model.UserSymptomReport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface UserSymptomReportRepository extends JpaRepository<UserSymptomReport, Integer> {

    @Query("SELECT usr FROM UserSymptomReport usr WHERE usr.user.id = :userId ORDER BY usr.reportedAt DESC")
    Page<UserSymptomReport> findByUserId(@Param("userId") String userId, Pageable pageable);

    @Query("SELECT usr FROM UserSymptomReport usr WHERE usr.sessionId = :sessionId ORDER BY usr.reportedAt")
    List<UserSymptomReport> findBySessionId(@Param("sessionId") String sessionId);

    @Query("SELECT usr FROM UserSymptomReport usr WHERE usr.user.id = :userId AND usr.sessionId = :sessionId")
    List<UserSymptomReport> findByUserIdAndSessionId(@Param("userId") String userId,
            @Param("sessionId") String sessionId);

    @Query("SELECT usr FROM UserSymptomReport usr WHERE usr.symptom.id = :symptomId")
    List<UserSymptomReport> findBySymptomId(@Param("symptomId") Integer symptomId);

    @Query("SELECT usr FROM UserSymptomReport usr WHERE usr.user.id = :userId AND usr.reportedAt >= :since")
    List<UserSymptomReport> findRecentByUserId(@Param("userId") String userId, @Param("since") LocalDateTime since);

    @Query("SELECT COUNT(usr) FROM UserSymptomReport usr WHERE usr.user.id = :userId")
    Long countByUserId(@Param("userId") String userId);

    @Query("SELECT DISTINCT usr.sessionId FROM UserSymptomReport usr WHERE usr.user.id = :userId ORDER BY MAX(usr.reportedAt) DESC")
    List<String> findDistinctSessionIdsByUserId(@Param("userId") String userId);

    // Additional query methods
    @Query("SELECT usr FROM UserSymptomReport usr WHERE usr.user = :user AND usr.sessionId = :sessionId ORDER BY usr.reportedAt DESC")
    List<UserSymptomReport> findByUserAndSessionId(@Param("user") User user, @Param("sessionId") String sessionId);
}
