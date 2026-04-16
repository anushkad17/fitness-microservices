package com.fitness.aiservice.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fitness.aiservice.model.Activity;
import com.fitness.aiservice.model.Recommendation;
import com.fitness.aiservice.repository.RecommendationRepository;
import com.fitness.aiservice.service.GeminiService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
public class ActivityConsumer {

    private final RecommendationRepository repository;
    private final GeminiService geminiService;

    public ActivityConsumer(RecommendationRepository repository,
                            GeminiService geminiService) {
        this.repository = repository;
        this.geminiService = geminiService;
    }

    @RabbitListener(queues = "activity.queue")
    public void consume(Activity activity) {

        System.out.println(" MESSAGE RECEIVED: " + activity);

        Recommendation recommendation = new Recommendation();

        recommendation.setActivityId(activity.getId());
        recommendation.setUserId(activity.getUserId());
        recommendation.setActivityType(activity.getType());

        //  PROMPT
        String prompt = """
    Analyze this fitness activity and respond ONLY with a valid JSON object.
    Do NOT include any explanation, markdown, or code blocks.
    The JSON must be perfectly valid with all arrays properly closed with ] not }.
    
    Activity: %s for %d minutes, %d calories burned.
    
    Return exactly this structure:
    {
      "recommendation": "one sentence summary",
      "improvements": ["improvement 1", "improvement 2", "improvement 3"],
      "suggestions": ["suggestion 1", "suggestion 2", "suggestion 3"],
      "safety": ["safety tip 1", "safety tip 2", "safety tip 3"]
    }
    """.formatted(activity.getType(), activity.getDuration(), activity.getCaloriesBurned());

        String aiResponse = geminiService.getAnswer(prompt);

        System.out.println(" RAW AI RESPONSE: " + aiResponse);

        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode outer = mapper.readTree(aiResponse);

            //  CHECK if Gemini returned valid response
            if (!outer.has("candidates")) {
                System.out.println(" Gemini error response: " + outer);

                recommendation.setRecommendation("AI unavailable right now");
                recommendation.setImprovements(Collections.emptyList());
                recommendation.setSuggestions(Collections.emptyList());
                recommendation.setSafety(Collections.emptyList());

                repository.save(recommendation);
                return;
            }

            //  Extract text
            String text = outer
                    .get("candidates")
                    .get(0)
                    .get("content")
                    .get("parts")
                    .get(0)
                    .get("text")
                    .asText();

            System.out.println(" EXTRACTED TEXT: " + text);


            text = text.replace("```json", "")
                    .replace("```", "")
                    .trim();

            JsonNode root = mapper.readTree(text);

            // ✅ SAFE FIELD EXTRACTION
            recommendation.setRecommendation(
                    root.has("recommendation") ? root.get("recommendation").asText()
                            : "No recommendation generated"
            );

            recommendation.setImprovements(
                    root.has("improvements")
                            ? mapper.convertValue(root.get("improvements"), List.class)
                            : Collections.emptyList()
            );

            recommendation.setSuggestions(
                    root.has("suggestions")
                            ? mapper.convertValue(root.get("suggestions"), List.class)
                            : Collections.emptyList()
            );

            recommendation.setSafety(
                    root.has("safety")
                            ? mapper.convertValue(root.get("safety"), List.class)
                            : Collections.emptyList()
            );

        } catch (Exception e) {
            System.out.println(" JSON PARSE FAILED → saving fallback");
            e.printStackTrace();

            recommendation.setRecommendation("AI parsing failed");
            recommendation.setImprovements(Collections.emptyList());
            recommendation.setSuggestions(Collections.emptyList());
            recommendation.setSafety(Collections.emptyList());
        }

        repository.save(recommendation);

        System.out.println("SAVED AI RECOMMENDATION");
    }
}