package com.fitness.gateway;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.*;

@Configuration
public class JwtDecoderConfig {
    @Bean
    public ReactiveJwtDecoder jwtDecoder() {
        NimbusReactiveJwtDecoder jwtDecoder =
                (NimbusReactiveJwtDecoder) ReactiveJwtDecoders.fromIssuerLocation(
                        "https://keycloak-deploy-st9e.onrender.com/realms/fitness-oauth2"
                );

        // ✅ ONLY validate issuer (ignore audience)
        OAuth2TokenValidator<Jwt> validator =
                JwtValidators.createDefaultWithIssuer(
                        "https://keycloak-deploy-st9e.onrender.com/realms/fitness-oauth2"
                );

        jwtDecoder.setJwtValidator(validator);

        return jwtDecoder;
    }
}
