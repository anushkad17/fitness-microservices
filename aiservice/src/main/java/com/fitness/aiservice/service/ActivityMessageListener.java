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

    @RabbitListener(queues = "activity.queue")
    public void processActivity(Activity activity) {
        try {
            log.info("📥 Received activity for AI processing. ID: {}", activity.getId());

            Recommendation recommendation = aiService.generateRecommendation(activity);

            if (recommendation != null) {
                recommendationRepository.save(recommendation);
                log.info("✅ AI Recommendation saved for activity: {}", activity.getId());
            }
        } catch (Exception e) {
            log.error("❌ Failed to process AI recommendation for activity {}: {}", activity.getId(), e.getMessage());
        }
    }
}
