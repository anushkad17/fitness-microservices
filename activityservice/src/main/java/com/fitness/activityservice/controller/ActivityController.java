package com.fitness.activityservice.controller;

import com.fitness.activityservice.dto.ActivityRequest;
import com.fitness.activityservice.dto.ActivityResponse;
import com.fitness.activityservice.service.ActivityService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/activities")
@AllArgsConstructor
@Slf4j
public class ActivityController {

    private final ActivityService activityService;

    // =========================
    // CREATE ACTIVITY
    // =========================
    @PostMapping
    public ResponseEntity<ActivityResponse> trackActivity(
            @RequestBody ActivityRequest request,
            Authentication authentication) {

        try {
            String userId;

            if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
                userId = jwt.getSubject();
            } else if (authentication != null) {
                userId = authentication.getName();
            } else {
                log.warn("Unauthorized POST /activities");
                return ResponseEntity.status(401).build();
            }

            request.setUserId(userId);

            return ResponseEntity.ok(activityService.trackActivity(request));

        } catch (Exception e) {
            log.error("Error in trackActivity", e);
            return ResponseEntity.status(500).build();
        }
    }

    // =========================
    // GET ALL ACTIVITIES
    // =========================
    @GetMapping
    public ResponseEntity<List<ActivityResponse>> getUserActivities(
            Authentication authentication) {

        try {
            String userId;

            if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
                userId = jwt.getSubject();
            } else if (authentication != null) {
                userId = authentication.getName();
            } else {
                log.warn("Unauthorized GET /activities");
                return ResponseEntity.status(401).build();
            }

            return ResponseEntity.ok(activityService.getUserActivities(userId));

        } catch (Exception e) {
            log.error("Error in getUserActivities", e);
            return ResponseEntity.status(500).build();
        }
    }

    // =========================
    // GET SINGLE ACTIVITY (FIXED)
    // =========================
    @GetMapping("/{activityId}")
    public ResponseEntity<ActivityResponse> getActivity(
            @PathVariable Long activityId) {   // ✅ FIXED (String → Long)

        try {
            return ResponseEntity.ok(activityService.getActivityById(activityId));
        } catch (Exception e) {
            log.error("Error fetching activity by id", e);
            return ResponseEntity.status(500).build();
        }
    }

    // =========================
    // DEBUG TEST ENDPOINT
    // =========================
    @GetMapping("/test")
    public ResponseEntity<String> test() {
        return ResponseEntity.ok("ACTIVITY SERVICE WORKING ✅");
    }
}

