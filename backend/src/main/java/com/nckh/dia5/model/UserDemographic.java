package com.nckh.dia5.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_demographics")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class UserDemographic {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private User user;

    @NotNull
    @Column(name = "birth_year", nullable = false)
    private Integer birthYear;

    @Column(name = "birth_month")
    private Integer birthMonth;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "gender", nullable = false)
    private Gender gender;

    @Column(name = "height_cm")
    private Integer heightCm;

    @Column(name = "weight_kg", precision = 5, scale = 2)
    private BigDecimal weightKg;

    @Enumerated(EnumType.STRING)
    @Column(name = "blood_type")
    private BloodType bloodType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "province_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Province province;

    @Size(max = 100)
    @Column(name = "occupation")
    private String occupation;

    @Enumerated(EnumType.STRING)
    @Column(name = "education_level")
    private EducationLevel educationLevel;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum Gender {
        male, female, other
    }

    public enum BloodType {
        @Column(name = "A+")
        A_POSITIVE("A+"),
        @Column(name = "A-")
        A_NEGATIVE("A-"),
        @Column(name = "B+")
        B_POSITIVE("B+"),
        @Column(name = "B-")
        B_NEGATIVE("B-"),
        @Column(name = "AB+")
        AB_POSITIVE("AB+"),
        @Column(name = "AB-")
        AB_NEGATIVE("AB-"),
        @Column(name = "O+")
        O_POSITIVE("O+"),
        @Column(name = "O-")
        O_NEGATIVE("O-"),
        unknown;

        private final String value;

        BloodType() {
            this.value = name();
        }

        BloodType(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    public enum EducationLevel {
        primary, secondary, high_school, bachelor, master, phd, other
    }
}
