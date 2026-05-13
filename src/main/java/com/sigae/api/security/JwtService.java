package com.sigae.api.security;

import com.sigae.api.config.SecurityProperties;
import com.sigae.api.model.entity.User;
import com.sigae.api.model.entity.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

  private final SecurityProperties securityProperties;

  public JwtService(SecurityProperties securityProperties) {
    this.securityProperties = securityProperties;
  }

  public String createAccessToken(User user) {
    Instant now = Instant.now();
    Instant expiration = now.plus(securityProperties.jwt().accessTokenTtl());

    return Jwts.builder()
        .issuer(securityProperties.jwt().issuer())
        .subject(user.getId().toString())
        .claim("userId", user.getId().toString())
        .claim("email", user.getEmail())
        .claim("role", user.getRole().name())
        .claim("locationIds", List.of())
        .issuedAt(Date.from(now))
        .expiration(Date.from(expiration))
        .signWith(getSigningKey())
        .compact();
  }

  public AuthenticatedUser parseAccessToken(String token) {
    try {
      Claims claims = Jwts.parser()
          .verifyWith(getSigningKey())
          .build()
          .parseSignedClaims(token)
          .getPayload();

      return new AuthenticatedUser(
          java.util.UUID.fromString(claims.get("userId", String.class)),
          claims.get("email", String.class),
          UserRole.valueOf(claims.get("role", String.class)),
          readStringListClaim(claims, "locationIds")
      );
    } catch (JwtException | IllegalArgumentException exception) {
      throw new JwtAuthenticationException("Token inválido o expirado.");
    }
  }

  public long getAccessTokenExpiresInSeconds() {
    return securityProperties.jwt().accessTokenTtl().toSeconds();
  }

  public SecurityProperties.Jwt properties() {
    return securityProperties.jwt();
  }

  private SecretKey getSigningKey() {
    return Keys.hmacShaKeyFor(securityProperties.jwt().secret().getBytes(StandardCharsets.UTF_8));
  }

  private List<String> readStringListClaim(Claims claims, String claimName) {
    Object value = claims.get(claimName);
    if (!(value instanceof List<?> rawValues)) {
      return List.of();
    }

    List<String> values = new ArrayList<>();
    for (Object rawValue : rawValues) {
      if (rawValue instanceof String stringValue) {
        values.add(stringValue);
      }
    }
    return List.copyOf(values);
  }
}
