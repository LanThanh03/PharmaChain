package com.nckh.dia5.model;

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
@Table(name = "ml_model_performance")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class MlModelPerformance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "model_name", nullable = false, length = 100)
    private String modelName;

    @Column(name = "model_version", nullable = false, length = 50)
    private String modelVersion;

    // Performance metrics
    @Column(name = "accuracy_rate", precision = 5, scale = 2)
    private BigDecimal accuracyRate;

    @Column(name = "precision_score", precision = 5, scale = 2)
    private BigDecimal precisionScore;

    @Column(name = "recall_score", precision = 5, scale = 2)
    private BigDecimal recallScore;

    @Column(name = "f1_score", precision = 5, scale = 2)
    private BigDecimal f1Score;

    // Usage statistics
    @Column(name = "total_predictions", nullable = false)
    private Integer totalPredictions = 0;

    @Column(name = "correct_predictions", nullable = false)
    private Integer correctPredictions = 0;

    @Column(name = "false_positives", nullable = false)
    private Integer falsePositives = 0;

    @Column(name = "false_negatives", nullable = false)
    private Integer falseNegatives = 0;

    // Performance by category
    @Column(name = "performance_by_specialty", columnDefinition = "TEXT")
    private String performanceBySpecialty; // JSON object

    @Column(name = "performance_by_severity", columnDefinition = "TEXT")
    private String performanceBySeverity; // JSON object

    @Column(name = "evaluation_date", nullable = false)
    private LocalDate evaluationDate;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
