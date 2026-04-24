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

@Component
@Slf4j
@RequiredArgsConstructor
public class KeycloakUserSyncFilter implements WebFilter {

    private final UserService userService;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        String token = exchange.getRequest().getHeaders().getFirst("Authorization");

        // ✅ FIX 1: Only run for API calls, ignore static assets/actuator
        if (!path.startsWith("/api") || token == null || !token.startsWith("Bearer ")) {
            return chain.filter(exchange);
        }

        RegisterRequest registerRequest = getUserDetails(token);
        if (registerRequest == null || registerRequest.getKeycloakId() == null) {
            return chain.filter(exchange);
        }

        String userId = registerRequest.getKeycloakId();

        // ✅ FIX 2: Run the sync in the background without waiting for it
        // We call subscribe() so it doesn't block the actual API request to Activities/AI
        userService.validateUser(userId)
                .flatMap(exist -> {
                    if (Boolean.FALSE.equals(exist)) {
                        log.info("Syncing user: {}", userId);
                        return userService.registerUser(registerRequest);
                    }
                    return Mono.empty();
                })
                .onErrorResume(e -> {
                    log.error("Background Sync Failed (likely 429): {}", e.getMessage());
                    return Mono.empty();
                })
                .subscribe(); // 🔥 This is the key: it runs async

        // Immediately continue to the actual microservice (Activity/AI)
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