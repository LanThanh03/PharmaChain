package com.nckh.dia5.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_symptom_reports")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class UserSymptomReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private User user;

    @Column(name = "session_id", length = 36)
    private String sessionId; // Groups symptoms reported in one conversation

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "symptom_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Symptom symptom;

    @Column(name = "severity")
    private Integer severity; // 1-10 scale or categorical

    @Column(name = "duration_hours")
    private Integer durationHours;

    @Enumerated(EnumType.STRING)
    @Column(name = "frequency")
    private Frequency frequency;

    @Column(name = "triggers", columnDefinition = "TEXT")
    private String triggers; // JSON array

    @Column(name = "associated_symptoms", columnDefinition = "TEXT")
    private String associatedSymptoms; // JSON array

    @Column(name = "location_body_part", length = 100)
    private String locationBodyPart;

    @Column(name = "quality_description", columnDefinition = "TEXT")
    private String qualityDescription; // "sharp", "dull", "burning", etc.

    @Enumerated(EnumType.STRING)
    @Column(name = "onset_type")
    private OnsetType onsetType;

    @CreatedDate
    @Column(name = "reported_at", nullable = false, updatable = false)
    private LocalDateTime reportedAt;

    public enum Frequency {
        constant, intermittent, occasional
    }

    public enum OnsetType {
        sudden, gradual
    }
}
