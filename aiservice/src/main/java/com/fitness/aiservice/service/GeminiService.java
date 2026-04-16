package com.fitness.aiservice.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Service
public class GeminiService {

    private final WebClient webClient = WebClient.builder().build();

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    // Hardcode the URL directly - bypass config server issues
    private static final String GEMINI_BASE_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-3-flash-preview:generateContent";

    public String getAnswer(String question) {
        String fullUrl = GEMINI_BASE_URL + "?key=" + geminiApiKey.trim();
        System.out.println("FINAL URL: " + fullUrl);

        Map<String, Object> requestBody = Map.of(
                "contents", List.of(
                        Map.of(
                                "parts", List.of(
                                        Map.of("text", question)
                                )
                        )
                )
        );

        try {
            String response = webClient.post()
                    .uri(fullUrl)
                    .header("Content-Type", "application/json")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            System.out.println(" RAW GEMINI RESPONSE: " + response);
            return response;

        } catch (Exception e) {
            e.printStackTrace();
            return "{\"error\":\"AI temporarily unavailable\"}";
        }
    }
}