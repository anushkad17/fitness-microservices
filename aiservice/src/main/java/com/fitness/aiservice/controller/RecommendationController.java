package com.fitness.aiservice.controller;

import com.fitness.aiservice.model.Recommendation;
import com.fitness.aiservice.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/recommendations")
public class RecommendationController {

    private final RecommendationService recommendationService;

    // =========================
    // GET BY USER
    // =========================
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Recommendation>> getUserRecommendation(
            @PathVariable String userId) {

        return ResponseEntity.ok(recommendationService.getByUserId(userId)); // ✅ FIXED
    }

    // =========================
    // GET BY ACTIVITY
    // =========================
    @GetMapping("/activity/{activityId}")
    public ResponseEntity<Recommendation> getActivityRecommendation(
            @PathVariable String activityId) {

        return ResponseEntity.ok(
                recommendationService.getByActivityId(activityId) // ✅ FIXED
        );
    }
}

