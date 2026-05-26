package com.nckh.dia5.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "news_articles")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class NewsArticle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "title", nullable = false, length = 500)
    private String title;

    @Column(name = "summary", columnDefinition = "TEXT")
    private String summary;

    @Column(name = "content", columnDefinition = "LONGTEXT")
    private String content;

    @Column(name = "url", length = 1000)
    private String url;

    @Column(name = "image_url", length = 1000)
    private String imageUrl;

    // Categorization
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "primary_category_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private MedicalSpecialty primaryCategory;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_audience")
    private TargetAudience targetAudience;

    @Enumerated(EnumType.STRING)
    @Column(name = "reading_level")
    private ReadingLevel readingLevel;

    // Source information
    @Column(name = "source_name", length = 100)
    private String sourceName = "suckhoedoisong.vn";

    @Column(name = "author", length = 200)
    private String author;

    @Column(name = "publication_date")
    private LocalDateTime publicationDate;

    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;

    // Content analysis
    @Column(name = "related_diseases", columnDefinition = "TEXT")
    private String relatedDiseases; // JSON array of disease_ids

    @Column(name = "related_symptoms", columnDefinition = "TEXT")
    private String relatedSymptoms; // JSON array of symptom_ids

    @Column(name = "fact_checked", nullable = false)
    private Boolean factChecked = false;

    @Column(name = "medical_accuracy_score", precision = 3, scale = 2)
    private BigDecimal medicalAccuracyScore; // 0-1 score

    // Engagement metrics
    @Column(name = "view_count", nullable = false)
    private Integer viewCount = 0;

    @Column(name = "share_count", nullable = false)
    private Integer shareCount = 0;

    @Column(name = "bookmark_count", nullable = false)
    private Integer bookmarkCount = 0;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "article", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private List<UserNewsInteraction> userInteractions;

    public enum TargetAudience {
        general_public, patients, professionals
    }

    public enum ReadingLevel {
        basic, intermediate, advanced
    }
}
