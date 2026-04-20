package com.fitness.aiservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fitness.aiservice.model.Recommendation;
import com.fitness.aiservice.repository.RecommendationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecommendationService {

    private final RecommendationRepository recommendationRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // =========================
    // SAVE RECOMMENDATION
    // =========================
    public Recommendation saveRecommendation(
            String activityId,
            String userId,
            String activityType,
            String recommendationText,
            List<String> improvements,
            List<String> suggestions,
            List<String> safety
    ) {

        try {
            Recommendation recommendation = Recommendation.builder()
                    .activityId(activityId)
                    .userId(userId)
                    .activityType(activityType)
                    .recommendation(recommendationText)

                    // ✅ Convert List → JSON String
                    .improvements(objectMapper.writeValueAsString(improvements))
                    .suggestions(objectMapper.writeValueAsString(suggestions))
                    .safety(objectMapper.writeValueAsString(safety))

                    .createdAt(LocalDateTime.now())
                    .build();

            return recommendationRepository.save(recommendation);

        } catch (Exception e) {
            log.error("❌ Failed to save recommendation", e);
            throw new RuntimeException("DB error while saving recommendation");
        }
    }

    // =========================
    // GET ALL
    // =========================
    public List<Recommendation> getAllRecommendations() {
        return recommendationRepository.findAll();
    }

    // =========================
    // GET BY USER
    // =========================
    public List<Recommendation> getByUserId(String userId) {
        return recommendationRepository.findByUserId(userId);
    }

    public Recommendation getByActivityId(String activityId) {
        return recommendationRepository.findByActivityId(activityId)
                .orElseThrow(() ->
                        new RuntimeException("Recommendation not found for activityId: " + activityId));
    }
}

