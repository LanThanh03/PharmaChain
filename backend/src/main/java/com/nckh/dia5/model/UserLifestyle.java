package com.nckh.dia5.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_lifestyle")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class UserLifestyle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    // Smoking
    @Enumerated(EnumType.STRING)
    @Column(name = "smoking_status", nullable = false)
    private SmokingStatus smokingStatus;

    @Column(name = "smoking_start_age")
    private Integer smokingStartAge;

    @Column(name = "smoking_quit_age")
    private Integer smokingQuitAge;

    @Column(name = "cigarettes_per_day")
    private Integer cigarettesPerDay;

    @Column(name = "years_smoked")
    private Integer yearsSmoked;

    // Alcohol
    @Enumerated(EnumType.STRING)
    @Column(name = "alcohol_frequency")
    private Frequency alcoholFrequency;

    @Column(name = "alcohol_units_per_week", precision = 4, scale = 1)
    private BigDecimal alcoholUnitsPerWeek;

    @Column(name = "alcohol_type_preference", columnDefinition = "TEXT")
    private String alcoholTypePreference; // JSON array

    // Physical Activity
    @Enumerated(EnumType.STRING)
    @Column(name = "exercise_frequency")
    private ExerciseFrequency exerciseFrequency;

    @Enumerated(EnumType.STRING)
    @Column(name = "exercise_intensity")
    private Intensity exerciseIntensity;

    @Column(name = "exercise_duration_minutes")
    private Integer exerciseDurationMinutes;

    @Column(name = "exercise_types", columnDefinition = "TEXT")
    private String exerciseTypes; // JSON array

    // Diet
    @Enumerated(EnumType.STRING)
    @Column(name = "diet_type")
    private DietType dietType;

    @Column(name = "meals_per_day")
    private Integer mealsPerDay = 3;

    @Column(name = "water_intake_liters", precision = 3, scale = 1)
    private BigDecimal waterIntakeLiters;

    // Sleep
    @Column(name = "sleep_hours_average", precision = 3, scale = 1)
    private BigDecimal sleepHoursAverage;

    @Enumerated(EnumType.STRING)
    @Column(name = "sleep_quality")
    private Quality sleepQuality;

    @Column(name = "sleep_disorders", columnDefinition = "TEXT")
    private String sleepDisorders; // JSON array

    // Stress and Mental Health
    @Enumerated(EnumType.STRING)
    @Column(name = "stress_level")
    private StressLevel stressLevel;

    @Enumerated(EnumType.STRING)
    @Column(name = "mental_health_status")
    private Quality mentalHealthStatus;

    // Occupational Hazards
    @Enumerated(EnumType.STRING)
    @Column(name = "work_environment")
    private WorkEnvironment workEnvironment;

    @Column(name = "chemical_exposure", nullable = false)
    private Boolean chemicalExposure = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "physical_demands")
    private PhysicalDemands physicalDemands;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum SmokingStatus {
        never, former, current
    }

    public enum Frequency {
        never, rarely, weekly, daily
    }

    public enum ExerciseFrequency {
        none, rare, weekly, daily
    }

    public enum Intensity {
        light, moderate, vigorous
    }

    public enum DietType {
        omnivore, vegetarian, vegan, keto, paleo, other
    }

    public enum Quality {
        poor, fair, good, excellent
    }

    public enum StressLevel {
        low, moderate, high, severe
    }

    public enum WorkEnvironment {
        office, outdoor, industrial, medical, other
    }

    public enum PhysicalDemands {
        sedentary, light, moderate, heavy
    }
}
