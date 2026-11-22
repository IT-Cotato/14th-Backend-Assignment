package com.cotato.backend14th;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistrar;

import java.security.SecureRandom;
import java.util.Base64;

@TestConfiguration(proxyBeanMethods = false)
public class JwtTestConfiguration {

    @Bean
    DynamicPropertyRegistrar jwtSigningKey() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        String signingKey = Base64.getEncoder().encodeToString(bytes);
        return registry -> registry.add("JWT_SIGNING_KEY", () -> signingKey);
    }
}
