package com.example.AudIon.controller.Auth;

import com.example.AudIon.service.Auth.AuthService;
import com.example.AudIon.service.Auth.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;
    private final JwtUtil jwtUtil;

    /**
     * Nonce 발급 (Web3 로그인 1단계)
     */
    @PostMapping("/nonce")
    public ResponseEntity<?> issueNonce(
            @RequestParam String walletAddress,
            HttpServletRequest request) {

        try {
            String clientIp = getClientIpAddress(request);
            String userAgent = request.getHeader("User-Agent");

            String nonce = authService.issueNonce(walletAddress, clientIp, userAgent);

            return ResponseEntity.ok(Map.of(
                    "nonce", nonce,
                    "message", "AudIon Login:\nnonce=" + nonce,
                    "expiresInMinutes", 5
            ));

        } catch (IllegalArgumentException e) {
            log.warn("Invalid wallet address for nonce request: {}", walletAddress);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));

        } catch (Exception e) {
            log.error("Failed to issue nonce for wallet: {}", walletAddress, e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to generate nonce"));
        }
    }

    /**
     * Web3 로그인 (서명 검증 후 JWT 발급)
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(
            @RequestParam String walletAddress,
            @RequestParam String signature,
            HttpServletRequest request) {

        try {
            String clientIp = getClientIpAddress(request);
            String userAgent = request.getHeader("User-Agent");

            String token = authService.login(walletAddress, signature, clientIp, userAgent);

            return ResponseEntity.ok(Map.of(
                    "token", token,
                    "type", "Bearer",
                    "walletAddress", walletAddress.toLowerCase(),
                    "expiresInHours", 12
            ));

        } catch (IllegalArgumentException e) {
            log.warn("Login failed for wallet {}: {}", walletAddress, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));

        } catch (Exception e) {
            log.error("Login error for wallet: {}", walletAddress, e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Login failed"));
        }
    }

    /**
     * 토큰 검증 및 사용자 정보 조회
     */
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(@RequestHeader("Authorization") String authHeader) {
        try {
            return jwtUtil.extractToken(authHeader)
                    .flatMap(jwtUtil::parseAndValidate)
                    .map(jws -> {
                        String userId = jwtUtil.getUserId(jws).orElse("");
                        String walletAddress = jwtUtil.getWalletAddress(jws).orElse("");
                        long remainingSeconds = jwtUtil.getRemainingLifetimeSeconds(jws);

                        return ResponseEntity.ok(Map.of(
                                "userId", userId,
                                "walletAddress", walletAddress,
                                "tokenRemainingSeconds", remainingSeconds,
                                "isValid", true
                        ));
                    })
                    .orElse(ResponseEntity.status(401).body(Map.of("error", "Invalid or expired token")));

        } catch (Exception e) {
            log.error("Error validating token", e);
            return ResponseEntity.status(401).body(Map.of("error", "Token validation failed"));
        }
    }

    /**
     * 로그아웃 (토큰 무효화는 클라이언트에서 처리)
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestHeader("Authorization") String authHeader) {
        try {
            // JWT는 상태가 없으므로 서버에서 직접 무효화하기 어려움
            // 필요시 블랙리스트 기능을 구현하거나, 클라이언트에서 토큰 삭제
            log.info("Logout requested");
            return ResponseEntity.ok(Map.of("message", "Logged out successfully"));

        } catch (Exception e) {
            log.error("Logout error", e);
            return ResponseEntity.ok(Map.of("message", "Logged out"));
        }
    }

    /**
     * Nonce 상태 확인 (디버깅용)
     */
    @GetMapping("/nonce/{walletAddress}")
    public ResponseEntity<?> getNonceStatus(@PathVariable String walletAddress) {
        try {
            var nonce = authService.getNonce(walletAddress);

            if (nonce == null) {
                return ResponseEntity.ok(Map.of("hasNonce", false));
            }

            return ResponseEntity.ok(Map.of(
                    "hasNonce", true,
                    "isValid", nonce.isValid(),
                    "isUsed", nonce.isUsed(),
                    "isExpired", nonce.isExpired(),
                    "secondsUntilExpiry", nonce.getSecondsUntilExpiry(),
                    "createdAt", nonce.getCreatedAt()
            ));

        } catch (Exception e) {
            log.error("Error checking nonce status for wallet: {}", walletAddress, e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to check nonce status"));
        }
    }

    /**
     * 클라이언트 IP 주소 추출 (프록시 고려)
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }
}