package com.fitness.activityservice.service;

import com.fitness.activityservice.ActivityRepository;
import com.fitness.activityservice.dto.ActivityRequest;
import com.fitness.activityservice.dto.ActivityResponse;
import com.fitness.activityservice.model.Activity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ActivityService {

    private final ActivityRepository activityRepository;
    private final UserValidationService userValidationService;
    private final RabbitTemplate rabbitTemplate;

    @Value("${rabbitmq.exchange.name}")
    private String exchange;

    @Value("${rabbitmq.routing.key}")
    private String routingKey;

    // ================================
    // CREATE ACTIVITY (FULL SAFE)
    // ================================
    public ActivityResponse trackActivity(ActivityRequest request) {

        log.info("Tracking activity for userId: {}", request.getUserId());

        // 🔒 SAFE USER VALIDATION
        try {
            userValidationService.validateUser(request.getUserId());
        } catch (Exception e) {
            log.warn("User validation skipped: {}", e.getMessage());
        }

        Activity savedActivity;

        try {
            Activity activity = Activity.builder()
                    .userId(request.getUserId())
                    .type(request.getType())
                    .duration(request.getDuration())
                    .caloriesBurned(request.getCaloriesBurned())
                    .startTime(request.getStartTime())
                    .additionalMetrics(request.getAdditionalMetrics())
                    .build();

            savedActivity = activityRepository.save(activity);

        } catch (Exception e) {
            log.error("❌ MongoDB SAVE FAILED", e);
            throw new RuntimeException("Database error while saving activity");
        }

        // 🔒 SAFE RABBITMQ
        try {
            rabbitTemplate.convertAndSend(exchange, routingKey, savedActivity);
        } catch (Exception e) {
            log.warn("RabbitMQ failed (ignored): {}", e.getMessage());
        }

        return mapToResponse(savedActivity);
    }

    // ================================
    // GET USER ACTIVITIES (SAFE)
    // ================================
    public List<ActivityResponse> getUserActivities(String userId) {

        log.info("Fetching activities for userId: {}", userId);

        List<Activity> activities;

        try {
            activities = activityRepository.findAll();
        } catch (Exception e) {
            log.error("❌ MongoDB FETCH FAILED", e);
            return Collections.emptyList();
        }

        if (activities == null || activities.isEmpty()) {
            log.warn("No activities found for userId: {}", userId);
            return Collections.emptyList();
        }

        return activities.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // ================================
    // GET BY ID
    // ================================
    public ActivityResponse getActivityById(String activityId) {
        return activityRepository.findById(activityId)
                .map(this::mapToResponse)
                .orElseThrow(() ->
                        new RuntimeException("Activity not found with id: " + activityId));
    }

    private ActivityResponse mapToResponse(Activity activity) {
        if (activity == null) return null;

        ActivityResponse response = new ActivityResponse();
        response.setId(activity.getId());
        response.setUserId(activity.getUserId());
        response.setType(activity.getType());
        response.setDuration(activity.getDuration());
        response.setCaloriesBurned(activity.getCaloriesBurned());
        response.setStartTime(activity.getStartTime());
        response.setAdditionalMetrics(activity.getAdditionalMetrics());
        response.setCreatedAt(activity.getCreatedAt());
        response.setUpdatedAt(activity.getUpdatedAt());

        return response;
    }
}