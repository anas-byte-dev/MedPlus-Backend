package com.medpulse.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
@Slf4j
public class JwtUtils {

    @Value("${medpulse.jwt.secret:}")
    private String jwtSecret;

    @Value("${medpulse.jwt.expiration-ms:86400000}")
    private long jwtExpirationMs;

    private SecretKey key() {
        if (jwtSecret == null || jwtSecret.isBlank()) {
            // Safe fallback for local testing without secrets; overridden by JWT_SECRET in .env
            return Keys.hmacShaKeyFor("DEFAULT_DEV_SECRET_KEY_FOR_LOCAL_ENVIRONMENT_ONLY_DO_NOT_USE_IN_PROD_123456".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }
        try {
            return Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));
        } catch (Exception e) {
            return Keys.hmacShaKeyFor(jwtSecret.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }
    }

    public String generateJwtToken(Authentication authentication) {
        UserDetailsImpl userPrincipal = (UserDetailsImpl) authentication.getPrincipal();

        return Jwts.builder()
                .subject(userPrincipal.getUsername())
                .claim("id", userPrincipal.getId())
                .claim("fullName", userPrincipal.getFullName())
                .claim("role", userPrincipal.getAuthorities().iterator().next().getAuthority())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
                .signWith(key())
                .compact();
    }

    public String getUserNameFromJwtToken(String token) {
        return Jwts.parser()
                .verifyWith(key())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    public String extractEmailFromToken(String token) {
        try {
            return getUserNameFromJwtToken(token);
        } catch (Exception e) {
            return extractEmailFromSupabaseToken(token);
        }
    }

    public String extractEmailFromSupabaseToken(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length >= 2) {
                String payloadJson = new String(java.util.Base64.getUrlDecoder().decode(parts[1]), java.nio.charset.StandardCharsets.UTF_8);
                com.fasterxml.jackson.databind.JsonNode node = new com.fasterxml.jackson.databind.ObjectMapper().readTree(payloadJson);
                if (node.has("exp")) {
                    long exp = node.get("exp").asLong();
                    if (exp * 1000 < System.currentTimeMillis()) {
                        log.warn("Supabase JWT token has expired");
                        return null;
                    }
                }
                if (node.has("email") && !node.get("email").asText().isBlank()) {
                    return node.get("email").asText();
                }
                if (node.has("user_metadata") && node.get("user_metadata").has("email")) {
                    return node.get("user_metadata").get("email").asText();
                }
            }
        } catch (Exception ex) {
            log.debug("Could not parse payload as Supabase token: {}", ex.getMessage());
        }
        return null;
    }

    public boolean validateJwtToken(String authToken) {
        if (authToken == null || authToken.isBlank()) return false;
        try {
            Jwts.parser().verifyWith(key()).build().parseSignedClaims(authToken);
            return true;
        } catch (Exception e) {
            return isValidSupabaseToken(authToken);
        }
    }

    public boolean isValidSupabaseToken(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length >= 2) {
                String payloadJson = new String(java.util.Base64.getUrlDecoder().decode(parts[1]), java.nio.charset.StandardCharsets.UTF_8);
                com.fasterxml.jackson.databind.JsonNode node = new com.fasterxml.jackson.databind.ObjectMapper().readTree(payloadJson);
                if (node.has("iss") && node.get("iss").asText().contains("supabase")) {
                    if (node.has("exp")) {
                        long exp = node.get("exp").asLong();
                        return (exp * 1000) > System.currentTimeMillis();
                    }
                    return true;
                }
            }
        } catch (Exception ignored) {}
        return false;
    }
}
