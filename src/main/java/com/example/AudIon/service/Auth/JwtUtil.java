package com.example.AudIon.service.Auth;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
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
public class JwtUtil {

    // 최소 32바이트 이상(권장: 충분히 긴 랜덤 바이트를 Base64 인코딩하여 설정)
    @Value("${jwt.secret:change-me-change-me-change-me-change-me-32+}")
    private String secret;

    // 만료시간(시간 단위)
    @Value("${jwt.exp-hours:12}")
    private long expHours;

    // 발급자(issuer)
    @Value("${jwt.issuer:AudIon}")
    private String issuer;

    // 키 버전(로테이션 대비)
    @Value("${jwt.kid:v1}")
    private String kid;

    @PostConstruct
    void validateConfig() {
        if ("change-me-change-me-change-me-change-me-32+".equals(secret)) {
            throw new IllegalStateException("Set a strong JWT secret via `jwt.secret`.");
        }
        if (expHours < 1 || expHours > 24 * 7) {
            throw new IllegalStateException("`jwt.exp-hours` must be between 1 and 168.");
        }
        // 키 길이 사전 검증 (Base64 또는 평문 모두 지원)
        // -> 실제 키 객체 생성은 매 호출 시 수행
        ensureStrongKey(secret);
    }

    /** 강한 키인지 검사 (Base64면 디코딩 후, 아니면 평문 바이트 길이로) */
    private static void ensureStrongKey(String secret) {
        byte[] keyBytes;
        try {
            keyBytes = Base64.getDecoder().decode(secret);
            if (keyBytes.length < 32) throw new IllegalArgumentException("JWT secret (base64) too short");
        } catch (IllegalArgumentException e) {
            keyBytes = secret.getBytes(StandardCharsets.UTF_8);
            if (keyBytes.length < 32) {
                throw new IllegalStateException("JWT secret must be >= 32 bytes (use a strong random secret or base64).");
            }
        }
    }

    /** 서명 키 생성 (매 호출 시 생성하지만 내부적으로 가벼움) */
    private Key signingKey() {
        byte[] keyBytes;
        try {
            keyBytes = Base64.getDecoder().decode(secret);
            if (keyBytes.length < 32) throw new IllegalArgumentException("JWT secret (base64) too short");
        } catch (IllegalArgumentException e) {
            keyBytes = secret.getBytes(StandardCharsets.UTF_8);
            if (keyBytes.length < 32) {
                throw new IllegalStateException("JWT secret must be >= 32 bytes (use a strong random secret or base64).");
            }
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /** 기본 만료시간(expHours)로 토큰 생성 */
    public String createToken(String userId, String wallet) {
        return createToken(userId, wallet, Duration.ofHours(expHours));
    }

    /** 만료시간을 커스텀 지정해 토큰 생성 */
    public String createToken(String userId, String wallet, Duration ttl) {
        Instant now = Instant.now();
        Instant exp = now.plus(ttl);
        String jti = UUID.randomUUID().toString();

        return Jwts.builder()
                .setHeaderParam("kid", kid)                   // 키 버전
                .setIssuer(issuer)                            // 발급자
                .setSubject(userId)                           // 사용자 ID
                .claim("wallet", wallet)                      // 지갑 주소
                .setId(jti)                                   // 토큰 고유 ID (블랙리스트/강제만료 등에 활용)
                .setNotBefore(Date.from(now.minusSeconds(5))) // 시계 오차 완충(5초)
                .setIssuedAt(Date.from(now))                  // 발급 시각
                .setExpiration(Date.from(exp))                // 만료 시각
                .signWith(signingKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /** Authorization 헤더에서 Bearer 토큰만 추출 */
    public Optional<String> resolveToken(String authHeader) {
        if (authHeader == null) return Optional.empty();
        String prefix = "Bearer ";
        if (authHeader.regionMatches(true, 0, prefix, 0, prefix.length())
                && authHeader.length() > prefix.length()) {
            String token = authHeader.substring(prefix.length()).trim();
            return token.isEmpty() ? Optional.empty() : Optional.of(token);
        }
        return Optional.empty();
    }

    /** 파싱 + 서명/만료/issuer 검증. 실패 시 Optional.empty() */
    public Optional<Jws<Claims>> parse(String token) {
        try {
            Jws<Claims> jws = Jwts.parserBuilder()
                    .requireIssuer(issuer)           // issuer 강제
                    .setAllowedClockSkewSeconds(60)  // 60초 시계 오차 허용
                    .setSigningKey(signingKey())
                    .build()
                    .parseClaimsJws(token);
            return Optional.of(jws);
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    public boolean isExpired(Claims claims) {
        Date exp = claims.getExpiration();
        return exp != null && exp.before(new Date());
    }

    public Optional<String> getWallet(Jws<Claims> jws) {
        Object w = jws.getBody().get("wallet");
        return (w instanceof String s && !s.isBlank()) ? Optional.of(s) : Optional.empty();
    }

    public Optional<String> getUserId(Jws<Claims> jws) {
        String sub = jws.getBody().getSubject();
        return (sub != null && !sub.isBlank()) ? Optional.of(sub) : Optional.empty();
    }
}
