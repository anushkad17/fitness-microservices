package com.fitness.aiservice.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fitness.aiservice.model.Activity;
import com.fitness.aiservice.model.Recommendation;
import com.fitness.aiservice.repository.RecommendationRepository;
import com.fitness.aiservice.service.GeminiService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Component
public class ActivityConsumer {

    private final RecommendationRepository repository;
    private final GeminiService geminiService;
    private final ObjectMapper objectMapper = new ObjectMapper(); // ✅ FIX

    public ActivityConsumer(RecommendationRepository repository,
                            GeminiService geminiService) {
        this.repository = repository;
        this.geminiService = geminiService;
    }

    @RabbitListener(queues = "activity.queue")
    public void consume(Activity activity) {

        System.out.println("📥 MESSAGE RECEIVED: " + activity);

        Recommendation recommendation = new Recommendation();

        recommendation.setActivityId(activity.getId());
        recommendation.setUserId(activity.getUserId());
        recommendation.setActivityType(
                activity.getType() != null ? activity.getType().toString() : "UNKNOWN"
        );

        // =========================
        // PROMPT
        // =========================
        String prompt = """
        Analyze this fitness activity and respond ONLY with a valid JSON object.
        Do NOT include explanation or markdown.

        Activity: %s for %d minutes, %d calories burned.

        Return:
        {
          "recommendation": "...",
          "improvements": ["..."],
          "suggestions": ["..."],
          "safety": ["..."]
        }
        """.formatted(
                activity.getType(),
                activity.getDuration(),
                activity.getCaloriesBurned()
        );

        String aiResponse = geminiService.getAnswer(prompt);

        System.out.println("🤖 RAW AI RESPONSE: " + aiResponse);

        try {
            JsonNode outer = objectMapper.readTree(aiResponse);

            // =========================
            // HANDLE GEMINI FAILURE
            // =========================
            if (!outer.has("candidates")) {
                System.out.println("⚠️ Gemini error response");

                recommendation.setRecommendation("AI unavailable right now");
                recommendation.setImprovements("[]");
                recommendation.setSuggestions("[]");
                recommendation.setSafety("[]");
                recommendation.setCreatedAt(LocalDateTime.now());

                repository.save(recommendation);
                return;
            }

            // =========================
            // EXTRACT TEXT
            // =========================
            String text = outer
                    .get("candidates")
                    .get(0)
                    .get("content")
                    .get("parts")
                    .get(0)
                    .get("text")
                    .asText();

            text = text.replace("```json", "")
                    .replace("```", "")
                    .trim();

            JsonNode root = objectMapper.readTree(text);

            // =========================
            // SAFE FIELD EXTRACTION
            // =========================
            recommendation.setRecommendation(
                    root.has("recommendation")
                            ? root.get("recommendation").asText()
                            : "No recommendation generated"
            );

            // ✅ LIST → JSON STRING (CRITICAL FIX)
            recommendation.setImprovements(
                    root.has("improvements")
                            ? objectMapper.writeValueAsString(
                            objectMapper.convertValue(root.get("improvements"), List.class)
                    )
                            : "[]"
            );

            recommendation.setSuggestions(
                    root.has("suggestions")
                            ? objectMapper.writeValueAsString(
                            objectMapper.convertValue(root.get("suggestions"), List.class)
                    )
                            : "[]"
            );

            recommendation.setSafety(
                    root.has("safety")
                            ? objectMapper.writeValueAsString(
                            objectMapper.convertValue(root.get("safety"), List.class)
                    )
                            : "[]"
            );

        } catch (Exception e) {
            System.out.println("❌ JSON PARSE FAILED → saving fallback");
            e.printStackTrace();

            recommendation.setRecommendation("AI parsing failed");
            recommendation.setImprovements("[]");
            recommendation.setSuggestions("[]");
            recommendation.setSafety("[]");
        }

        // =========================
        // FINAL SAVE
        // =========================
        recommendation.setCreatedAt(LocalDateTime.now());

        repository.save(recommendation);

        System.out.println("✅ SAVED AI RECOMMENDATION");
    }
}

