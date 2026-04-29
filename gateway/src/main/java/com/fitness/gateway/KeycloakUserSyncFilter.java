package com.fitness.gateway;

import com.fitness.gateway.user.RegisterRequest;
import com.fitness.gateway.user.UserService;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Component
@Slf4j
@RequiredArgsConstructor
public class KeycloakUserSyncFilter implements WebFilter {

    private final UserService userService;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        String token = exchange.getRequest().getHeaders().getFirst("Authorization");

        // 1. Only run for API calls and ensure we have a Bearer token
        if (!path.startsWith("/api") || token == null || !token.startsWith("Bearer ")) {
            return chain.filter(exchange);
        }

        RegisterRequest registerRequest = getUserDetails(token);

        // 2. If parsing fails, just continue the request chain
        if (registerRequest == null || registerRequest.getKeycloakId() == null) {
            return chain.filter(exchange);
        }

        String userId = registerRequest.getKeycloakId();

        // 3. ASYNCHRONOUS BACKGROUND SYNC
        // We do NOT return this Mono. We subscribe on a separate thread pool
        // to ensure the main request to ActivityService/AIService proceeds immediately.
        userService.validateUser(userId)
                .flatMap(exist -> {
                    if (Boolean.FALSE.equals(exist)) {
                        log.info("User {} not found in local database. Triggering background registration...", userId);
                        return userService.registerUser(registerRequest);
                    }
                    return Mono.empty();
                })
                .subscribeOn(Schedulers.boundedElastic()) // Run on a background thread pool
                .onErrorResume(e -> {
                    log.error("Background User Sync Failed: {}. Request continues.", e.getMessage());
                    return Mono.empty();
                })
                .subscribe(); // Execute fire-and-forget

        // 4. Immediately continue the main filter chain
        return chain.filter(exchange);
    }

    private RegisterRequest getUserDetails(String token) {
        try {
            // Remove 'Bearer ' prefix and trim whitespace
            String tokenWithoutBearer = token.substring(7).trim();
            SignedJWT signedJWT = SignedJWT.parse(tokenWithoutBearer);
            JWTClaimsSet claims = signedJWT.getJWTClaimsSet();

            RegisterRequest registerRequest = new RegisterRequest();
            registerRequest.setEmail(claims.getStringClaim("email"));
            registerRequest.setKeycloakId(claims.getStringClaim("sub"));

            // Password is dummy because authentication is handled by Keycloak
            registerRequest.setPassword("dummy@123123");
            registerRequest.setFirstName(claims.getStringClaim("given_name"));
            registerRequest.setLastName(claims.getStringClaim("family_name"));

            return registerRequest;

        } catch (Exception e) {
            log.error("Error parsing JWT for user sync", e);
            return null;
        }
    }
}