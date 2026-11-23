package com.cotato.backend14th.global.security;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.time.*;
import java.util.*;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TokenService {
    private static final String ISSUER = "cotato-study";
    private final JwtEncoder encoder;
    private final JwtDecoder decoder;
    private final RevokedTokenRepository revoked;

    public TokenService(@Value("${JWT_SECRET}") String secret, RevokedTokenRepository revoked) {
        byte[] bytes = Base64.getDecoder().decode(secret);
        if (bytes.length < 32) {
            throw new IllegalArgumentException("JWT_SECRET must encode at least 32 bytes");
        }
        var key = new SecretKeySpec(bytes, "HmacSHA256");
        this.encoder = new NimbusJwtEncoder(new ImmutableSecret<>(key));
        var jwtDecoder = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
        jwtDecoder.setJwtValidator(new org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator<>(
                new JwtIssuerValidator(ISSUER), new JwtTimestampValidator(Duration.ZERO)));
        this.decoder = jwtDecoder;
        this.revoked = revoked;
    }

    public String issue(long userId) {
        Instant now = Instant.now();
        var claims = JwtClaimsSet.builder().issuer(ISSUER).subject(Long.toString(userId))
                .id(UUID.randomUUID().toString()).issuedAt(now).expiresAt(now.plus(Duration.ofHours(1))).build();
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
    }

    public Jwt authenticate(String token) {
        Jwt jwt = decoder.decode(token);
        if (jwt.getId() == null || jwt.getExpiresAt() == null || jwt.getSubject() == null || revoked.existsById(jwt.getId())) {
            throw new BadJwtException("Invalid access token");
        }
        return jwt;
    }

    @Transactional
    public void revoke(Jwt jwt) {
        revoked.deleteByExpiresAtBefore(Instant.now());
        revoked.revoke(jwt.getId(), jwt.getExpiresAt());
    }
}
