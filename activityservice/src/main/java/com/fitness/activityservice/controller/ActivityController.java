package com.fitness.activityservice.controller;

import com.fitness.activityservice.dto.ActivityRequest;
import com.fitness.activityservice.dto.ActivityResponse;
import com.fitness.activityservice.service.ActivityService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/activities")
@AllArgsConstructor
public class ActivityController {

    private ActivityService activityService;

    //  CREATE ACTIVITY
    @PostMapping
    public ResponseEntity<ActivityResponse> trackActivity(
            @RequestBody ActivityRequest request,
            Authentication authentication) {

        Jwt jwt = (Jwt) authentication.getPrincipal();
        String userId = jwt.getSubject(); //  Extract from token

        request.setUserId(userId);

        return ResponseEntity.ok(activityService.trackActivity(request));
    }

    //  GET ALL ACTIVITIES FOR USER
    @GetMapping
    public ResponseEntity<List<ActivityResponse>> getUserActivities(
            Authentication authentication) {

        String userId = null;

        if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
            userId = jwt.getSubject();
        } else {
            throw new RuntimeException("Unauthorized: JWT missing");
        } //  Extract from token

        return ResponseEntity.ok(activityService.getUserActivities(userId));
    }

    //  GET SINGLE ACTIVITY
    @GetMapping("/{activityId}")
    public ResponseEntity<ActivityResponse> getActivity(
            @PathVariable String activityId) {

        return ResponseEntity.ok(activityService.getActivityById(activityId));
    }
}