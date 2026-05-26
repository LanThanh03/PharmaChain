package com.nckh.dia5.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "allergen_categories")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class AllergenCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotNull
    @Size(max = 50)
    @Column(name = "category", nullable = false)
    private String category; // 'drug', 'food', 'environmental', 'contact'

    @NotNull
    @Size(max = 200)
    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "common_symptoms", columnDefinition = "TEXT")
    private String commonSymptoms; // JSON array

    @Column(name = "severity_levels", columnDefinition = "TEXT")
    private String severityLevels; // JSON array

    @Column(name = "cross_reactions", columnDefinition = "TEXT")
    private String crossReactions; // JSON array

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
