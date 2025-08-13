package com.example.AudIon.service.Auth;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

@Component
@Slf4j
public class JwtUtil {

    @Value("${jwt.secret:change-me-change-me-change-me-change-me-32+}")
    private String secret;

    @Value("${jwt.exp-hours:12}")
    private long expHours;

    @Value("${jwt.issuer:AudIon}")
    private String issuer;

    @Value("${jwt.kid:v1}")
    private String keyId;

    private Key signingKey;

    @PostConstruct
    void init() {
        validateConfiguration();
        this.signingKey = createSigningKey();
        log.info("JWT utility initialized with issuer: {} and expiry: {} hours", issuer, expHours);
    }

    private void validateConfiguration() {
        // 기본값 사용 금지
        if ("change-me-change-me-change-me-change-me-32+".equals(secret)) {
            throw new IllegalStateException("JWT secret must be changed from default value. Set jwt.secret property.");
        }

        // 만료 시간 검증
        if (expHours < 1 || expHours > 24 * 7) { // 1시간 ~ 1주일
            throw new IllegalStateException("JWT expiry hours must be between 1 and 168 (1 week)");
        }

        // 키 강도 검증
        validateSecretStrength(secret);
    }

    private void validateSecretStrength(String secret) {
        byte[] keyBytes = getKeyBytes(secret);
        if (keyBytes.length < 32) {
            throw new IllegalStateException("JWT secret must be at least 32 bytes long for security");
        }
    }

    private Key createSigningKey() {
        byte[] keyBytes = getKeyBytes(secret);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    private byte[] getKeyBytes(String secret) {
        try {
            // Base64 디코딩 시도
            byte[] decoded = Base64.getDecoder().decode(secret);
            if (decoded.length >= 32) {
                return decoded;
            }
            // Base64이지만 너무 짧으면 원본 문자열 사용
            return secret.getBytes(StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            // Base64가 아니면 원본 문자열을 UTF-8로 인코딩
            return secret.getBytes(StandardCharsets.UTF_8);
        }
    }

    /**
     * 기본 만료시간으로 JWT 토큰 생성
     */
    public String createToken(String userId, String walletAddress) {
        return createToken(userId, walletAddress, Duration.ofHours(expHours));
    }

    /**
     * 커스텀 만료시간으로 JWT 토큰 생성
     */
    public String createToken(String userId, String walletAddress, Duration ttl) {
        Instant now = Instant.now();
        Instant expiration = now.plus(ttl);
        String jwtId = UUID.randomUUID().toString();

        try {
            return Jwts.builder()
                    .setHeaderParam("kid", keyId)                    // 키 ID
                    .setHeaderParam("typ", "JWT")                    // 토큰 타입
                    .setIssuer(issuer)                              // 발급자
                    .setSubject(userId)                             // 주체 (사용자 ID)
                    .setAudience("AudIon-App")                      // 대상
                    .claim("wallet", walletAddress)                  // 지갑 주소
                    .claim("type", "access")                        // 토큰 타입
                    .setId(jwtId)                                   // JWT ID
                    .setNotBefore(Date.from(now.minusSeconds(5)))   // 5초 전부터 유효 (시계 오차 대응)
                    .setIssuedAt(Date.from(now))                    // 발급 시각
                    .setExpiration(Date.from(expiration))           // 만료 시각
                    .signWith(signingKey, SignatureAlgorithm.HS256)
                    .compact();

        } catch (Exception e) {
            log.error("Failed to create JWT token for user: {}", userId, e);
            throw new RuntimeException("Token creation failed", e);
        }
    }

    /**
     * Authorization 헤더에서 Bearer 토큰 추출
     */
    public Optional<String> extractToken(String authorizationHeader) {
        if (authorizationHeader == null || authorizationHeader.trim().isEmpty()) {
            return Optional.empty();
        }

        String prefix = "Bearer ";
        if (authorizationHeader.regionMatches(true, 0, prefix, 0, prefix.length())
                && authorizationHeader.length() > prefix.length()) {
            String token = authorizationHeader.substring(prefix.length()).trim();
            return token.isEmpty() ? Optional.empty() : Optional.of(token);
        }

        return Optional.empty();
    }

    /**
     * JWT 토큰 파싱 및 검증
     */
    public Optional<Jws<Claims>> parseAndValidate(String token) {
        try {
            Jws<Claims> jws = Jwts.parserBuilder()
                    .requireIssuer(issuer)                          // 발급자 검증
                    .requireAudience("AudIon-App")                  // 대상 검증
                    .setAllowedClockSkewSeconds(60)                 // 60초 시계 오차 허용
                    .setSigningKey(signingKey)
                    .build()
                    .parseClaimsJws(token);

            // 추가 검증
            Claims claims = jws.getBody();
            if (isTokenExpired(claims)) {
                log.debug("Token is expired");
                return Optional.empty();
            }

            return Optional.of(jws);

        } catch (ExpiredJwtException e) {
            log.debug("Token is expired: {}", e.getMessage());
            return Optional.empty();
        } catch (UnsupportedJwtException e) {
            log.warn("Unsupported JWT token: {}", e.getMessage());
            return Optional.empty();
        } catch (MalformedJwtException e) {
            log.warn("Malformed JWT token: {}", e.getMessage());
            return Optional.empty();
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Invalid JWT token: {}", e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * 토큰 만료 여부 확인
     */
    public boolean isTokenExpired(Claims claims) {
        Date expiration = claims.getExpiration();
        return expiration != null && expiration.before(new Date());
    }

    /**
     * 토큰에서 지갑 주소 추출
     */
    public Optional<String> getWalletAddress(Jws<Claims> jws) {
        Object wallet = jws.getBody().get("wallet");
        if (wallet instanceof String walletStr && !walletStr.trim().isEmpty()) {
            return Optional.of(walletStr.trim().toLowerCase());
        }
        return Optional.empty();
    }

    /**
     * 토큰에서 사용자 ID 추출
     */
    public Optional<String> getUserId(Jws<Claims> jws) {
        String subject = jws.getBody().getSubject();
        if (subject != null && !subject.trim().isEmpty()) {
            return Optional.of(subject.trim());
        }
        return Optional.empty();
    }

    /**
     * 토큰에서 JWT ID 추출
     */
    public Optional<String> getJwtId(Jws<Claims> jws) {
        String jwtId = jws.getBody().getId();
        if (jwtId != null && !jwtId.trim().isEmpty()) {
            return Optional.of(jwtId.trim());
        }
        return Optional.empty();
    }

    /**
     * 토큰 발급 시간 조회
     */
    public Optional<Instant> getIssuedAt(Jws<Claims> jws) {
        Date issuedAt = jws.getBody().getIssuedAt();
        return issuedAt != null ? Optional.of(issuedAt.toInstant()) : Optional.empty();
    }

    /**
     * 토큰 만료 시간 조회
     */
    public Optional<Instant> getExpirationTime(Jws<Claims> jws) {
        Date expiration = jws.getBody().getExpiration();
        return expiration != null ? Optional.of(expiration.toInstant()) : Optional.empty();
    }

    /**
     * 토큰 남은 수명 (초)
     */
    public long getRemainingLifetimeSeconds(Jws<Claims> jws) {
        return getExpirationTime(jws)
                .map(expiration -> {
                    long remaining = Instant.now().until(expiration, java.time.temporal.ChronoUnit.SECONDS);
                    return Math.max(0, remaining);
                })
                .orElse(0L);
    }

    /**
     * 토큰 정보 요약 (디버깅용)
     */
    public String getTokenSummary(Jws<Claims> jws) {
        Claims claims = jws.getBody();
        return String.format("JWT{userId=%s, wallet=%s, jti=%s, exp=%s}",
                claims.getSubject(),
                claims.get("wallet"),
                claims.getId(),
                claims.getExpiration());
    }
}