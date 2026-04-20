package com.fitness.aiservice.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "ai_recommendations")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Recommendation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String activityId;
    private String userId;
    private String activityType;

    @Column(columnDefinition = "TEXT")
    private String recommendation;

    // ✅ Store lists as JSON/String
    @Column(columnDefinition = "TEXT")
    private String improvements;

    @Column(columnDefinition = "TEXT")
    private String suggestions;

    @Column(columnDefinition = "TEXT")
    private String safety;

    private LocalDateTime createdAt;
}

