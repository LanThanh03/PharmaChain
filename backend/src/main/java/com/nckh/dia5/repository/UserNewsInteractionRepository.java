package com.nckh.dia5.repository;

import com.nckh.dia5.model.UserNewsInteraction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserNewsInteractionRepository extends JpaRepository<UserNewsInteraction, Integer> {

    @Query("SELECT uni FROM UserNewsInteraction uni WHERE uni.user.id = :userId ORDER BY uni.interactionTimestamp DESC")
    List<UserNewsInteraction> findByUserId(@Param("userId") String userId);

    @Query("SELECT uni FROM UserNewsInteraction uni WHERE uni.article.id = :articleId")
    List<UserNewsInteraction> findByArticleId(@Param("articleId") Integer articleId);

    @Query("SELECT uni FROM UserNewsInteraction uni WHERE uni.user.id = :userId AND uni.article.id = :articleId")
    List<UserNewsInteraction> findByUserIdAndArticleId(@Param("userId") String userId,
            @Param("articleId") Integer articleId);

    @Query("SELECT uni FROM UserNewsInteraction uni WHERE uni.user.id = :userId AND uni.interactionType = :type")
    List<UserNewsInteraction> findByUserIdAndType(@Param("userId") String userId,
            @Param("type") UserNewsInteraction.InteractionType type);

    Optional<UserNewsInteraction> findByUserIdAndArticleIdAndInteractionType(String userId, Integer articleId,
            UserNewsInteraction.InteractionType type);

    @Query("SELECT uni FROM UserNewsInteraction uni WHERE uni.interactionTimestamp >= :since")
    List<UserNewsInteraction> findInteractionsSince(@Param("since") LocalDateTime since);

    @Query("SELECT COUNT(uni) FROM UserNewsInteraction uni WHERE uni.article.id = :articleId AND uni.interactionType = :type")
    Long countByArticleIdAndType(@Param("articleId") Integer articleId,
            @Param("type") UserNewsInteraction.InteractionType type);

    @Query("SELECT AVG(uni.readingTimeSeconds) FROM UserNewsInteraction uni WHERE uni.interactionType = 'view' AND uni.readingTimeSeconds > 0")
    Double findAverageReadingTime();
}
