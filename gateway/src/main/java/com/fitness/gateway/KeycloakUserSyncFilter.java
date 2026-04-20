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

        String token = exchange.getRequest().getHeaders().getFirst("Authorization");

        // 1. If no token or invalid format → continue the request chain immediately
        if (token == null || !token.startsWith("Bearer ")) {
            return chain.filter(exchange);
        }

        RegisterRequest registerRequest = getUserDetails(token);

        // 2. If parsing fails or required ID is missing → continue request
        if (registerRequest == null || registerRequest.getKeycloakId() == null) {
            return chain.filter(exchange);
        }

        String userId = registerRequest.getKeycloakId();

        // 3. Execute User Sync with Error Resilience
        return userService.validateUser(userId)
                .flatMap(exist -> {
                    // Using Boolean.FALSE.equals for null-safety
                    if (Boolean.FALSE.equals(exist)) {
                        log.info("User {} not found in local database. Triggering registration sync...", userId);
                        return userService.registerUser(registerRequest).then();
                    }
                    return Mono.empty();
                })
                /* * ✅ CRITICAL FIX:
                 * If the User Service call fails (502, 503, or Connection Refused),
                 * we catch it here and return Mono.empty(). This prevents the
                 * Gateway from returning a 500 error to the browser.
                 */
                .onErrorResume(e -> {
                    log.error("User Sync Background Process Failed: {}. Continuing main request.", e.getMessage());
                    return Mono.empty();
                })
                .then(chain.filter(exchange));
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