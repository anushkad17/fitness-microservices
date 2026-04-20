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

        if (!(authentication != null && authentication.getPrincipal() instanceof Jwt jwt)) {
            log.warn("Unauthorized POST /activities");
            return ResponseEntity.status(401).build();  // ✅ no 500
        }

        String userId = jwt.getSubject();
        request.setUserId(userId);

        return ResponseEntity.ok(activityService.trackActivity(request));
    }

    // =========================
    // GET ALL ACTIVITIES
    // =========================
    @GetMapping
    public ResponseEntity<List<ActivityResponse>> getUserActivities(
            Authentication authentication) {

        if (!(authentication != null && authentication.getPrincipal() instanceof Jwt jwt)) {
            log.warn("Unauthorized GET /activities");
            return ResponseEntity.status(401).build();  // ✅ no 500
        }

        String userId = jwt.getSubject();

        return ResponseEntity.ok(activityService.getUserActivities(userId));
    }

    // =========================
    // GET BY ID
    // =========================
    @GetMapping("/{activityId}")
    public ResponseEntity<ActivityResponse> getActivity(
            @PathVariable String activityId) {

        return ResponseEntity.ok(activityService.getActivityById(activityId));
    }

    @GetMapping("/test")
    public String test() {
        return "NEW CODE DEPLOYED";
    }
}