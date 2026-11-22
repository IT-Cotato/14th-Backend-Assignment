package com.cotato.backend14th.global.config;

import com.cotato.backend14th.global.security.RevokedTokens;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

@Configuration
public class JwtConfig {

    @Bean
    SecretKey accessTokenSigningKey(@Value("${JWT_SIGNING_KEY:}") String signingKey) {
        byte[] key;
        try {
            key = Base64.getDecoder().decode(signingKey);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("JWT_SIGNING_KEY must be Base64-encoded.");
        }
        if (key.length < 32) {
            throw new IllegalStateException("JWT_SIGNING_KEY must contain at least 32 bytes after Base64 decoding.");
        }

        return new SecretKeySpec(key, "HmacSHA256");
    }

    @Bean
    public JwtEncoder jwtEncoder(SecretKey accessTokenSigningKey) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(accessTokenSigningKey));
    }

    @Bean
    public JwtDecoder jwtDecoder(SecretKey accessTokenSigningKey, RevokedTokens revokedTokens) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(accessTokenSigningKey).build();
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                new JwtTimestampValidator(Duration.ZERO),
                new JwtClaimValidator<Instant>("exp", exp -> exp != null && exp.isAfter(Instant.now())),
                new JwtClaimValidator<String>("sub", sub -> sub != null && !sub.isBlank()),
                jwt -> revokedTokens.isRevoked(jwt)
                        ? OAuth2TokenValidatorResult.failure(
                                new OAuth2Error("invalid_token"))
                        : OAuth2TokenValidatorResult.success()
        ));
        return decoder;
    }
}
