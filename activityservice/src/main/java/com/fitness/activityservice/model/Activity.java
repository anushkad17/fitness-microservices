package com.fitness.activityservice.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity   // ✅ MUST HAVE
@Table(name = "activities")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Activity {

    @Id   // ✅ JPA ID
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String userId;

    @Enumerated(EnumType.STRING)
    private ActivityType type;

    private Integer duration;
    private Integer caloriesBurned;
    private LocalDateTime startTime;

    @Column(columnDefinition = "TEXT")
    private String additionalMetrics;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}