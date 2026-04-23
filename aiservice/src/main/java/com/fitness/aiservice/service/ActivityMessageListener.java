package com.fitness.aiservice.service;

import com.fitness.aiservice.model.Activity;
import com.fitness.aiservice.model.Recommendation;
import com.fitness.aiservice.repository.RecommendationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class ActivityMessageListener {

    private final ActivityAIService aiService;
    private final RecommendationRepository recommendationRepository;

    @RabbitListener(queues = "${rabbitmq.queue.name}")  // ✅ Use property
    public void processActivity(Activity activity) {
        log.info("====================================");
        log.info("📥 RECEIVED MESSAGE FROM RABBITMQ");
        log.info("Activity ID: {}", activity.getId());
        log.info("Activity Type: {}", activity.getType());
        log.info("User ID: {}", activity.getUserId());
        log.info("====================================");

        try {
            Recommendation recommendation = aiService.generateRecommendation(activity);

            if (recommendation != null) {
                Recommendation saved = recommendationRepository.save(recommendation);
                log.info("✅ Saved recommendation ID: {} for activity: {}",
                        saved.getId(), activity.getId());
            } else {
                log.warn("⚠️ Null recommendation returned for activity: {}",
                        activity.getId());
            }
        } catch (Exception e) {
            log.error("❌ Failed to process activity {}: {}",
                    activity.getId(), e.getMessage(), e);
        }
    }
}