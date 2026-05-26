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
@Table(name = "symptoms")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Symptom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotNull
    @Size(max = 200)
    @Column(name = "name", nullable = false)
    private String name;

    @Size(max = 100)
    @Column(name = "category")
    private String category; // 'respiratory', 'cardiovascular', 'neurological', etc.

    @Enumerated(EnumType.STRING)
    @Column(name = "severity_scale")
    private SeverityScale severityScale;

    @Size(max = 50)
    @Column(name = "measurement_unit")
    private String measurementUnit; // 'degrees', 'frequency', 'intensity', etc.

    @Column(name = "related_body_systems", columnDefinition = "TEXT")
    private String relatedBodySystems; // JSON array

    @Column(name = "common_causes", columnDefinition = "TEXT")
    private String commonCauses; // JSON array

    @Column(name = "red_flag_indicators", columnDefinition = "TEXT")
    private String redFlagIndicators; // JSON array

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public enum SeverityScale {
        @Column(name = "1-10")
        ONE_TO_TEN("1-10"),
        @Column(name = "mild-severe")
        MILD_SEVERE("mild-severe"),
        @Column(name = "absent-present")
        ABSENT_PRESENT("absent-present");

        private final String value;

        SeverityScale(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }
}
