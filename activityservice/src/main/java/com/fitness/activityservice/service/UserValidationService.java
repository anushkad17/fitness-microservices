package com.fitness.activityservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserValidationService {

    private final WebClient userServiceWebClient;

    public boolean validateUser(String userId) {
        log.info("Calling User Validation API for userId: {}", userId);

        try {
            Boolean response = userServiceWebClient.get()
                    .uri("/api/users/{userId}/validate", userId)
                    .retrieve()
                    .bodyToMono(Boolean.class)
                    .block();

            return response != null && response;

        } catch (WebClientResponseException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                log.warn("User not found: {}", userId);
            } else if (e.getStatusCode() == HttpStatus.BAD_REQUEST) {
                log.warn("Invalid request for userId: {}", userId);
            } else {
                log.error("Error validating user: {}", e.getMessage());
            }
        } catch (Exception e) {
            log.error("Unexpected error during user validation: {}", e.getMessage());
        }

        return false; // ✅ safe fallback
    }
}