package com.nckh.dia5.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_analytics", uniqueConstraints = @UniqueConstraint(columnNames = { "user_id", "session_date" }))
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class UserAnalytics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private User user;

    @Column(name = "session_date", nullable = false)
    private LocalDate sessionDate;

    // Usage patterns
    @Column(name = "total_sessions", nullable = false)
    private Integer totalSessions = 0;

    @Column(name = "total_time_minutes", nullable = false)
    private Integer totalTimeMinutes = 0;

    @Column(name = "features_used", columnDefinition = "TEXT")
    private String featuresUsed; // JSON array

    // Health engagement
    @Column(name = "symptoms_reported", nullable = false)
    private Integer symptomsReported = 0;

    @Column(name = "diagnoses_received", nullable = false)
    private Integer diagnosesReceived = 0;

    @Column(name = "articles_read", nullable = false)
    private Integer articlesRead = 0;

    @Column(name = "recommendations_followed", nullable = false)
    private Integer recommendationsFollowed = 0;

    // AI interaction quality
    @Column(name = "average_satisfaction", precision = 3, scale = 2)
    private BigDecimal averageSatisfaction;

    @Column(name = "helpful_responses", nullable = false)
    private Integer helpfulResponses = 0;

    @Column(name = "total_responses", nullable = false)
    private Integer totalResponses = 0;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
