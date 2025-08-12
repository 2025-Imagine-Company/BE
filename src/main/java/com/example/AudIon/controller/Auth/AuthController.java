package com.example.AudIon.controller.Auth;

import com.example.AudIon.dto.auth.*;
import com.example.AudIon.service.Auth.AuthService;
import com.example.AudIon.service.Auth.JwtUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final JwtUtil jwt;

    @PostMapping("/nonce")
    public ResponseEntity<NonceResponse> issueNonce(@Valid @RequestBody NonceRequest req) {
        // 서비스에서 normalize 수행
        String nonce = authService.issueNonce(req.getWalletAddress());
        String message = "AudIon Login:\nnonce=" + nonce;
        Instant expiresAt = Instant.now().plus(Duration.ofMinutes(5));

        String normalized = req.getWalletAddress().toLowerCase(); // or 서비스/유틸 결과 사용
        return ResponseEntity.ok(
                NonceResponse.builder()
                        .walletAddress(normalized)
                        .nonce(nonce)
                        .message(message)
                        .expiresAt(expiresAt)
                        .build()
        );
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest req) {
        String token = authService.login(req.getWalletAddress(), req.getSignature());

        var jws = jwt.parse(token).orElseThrow(); // 서명/issuer/exp 검증
        var exp = jws.getBody().getExpiration().toInstant();
        var userId = jwt.getUserId(jws).map(UUID::fromString).orElse(null);
        var wallet = jwt.getWallet(jws).orElse(null);

        return ResponseEntity.ok(
                LoginResponse.builder()
                        .accessToken(token)
                        .tokenType("Bearer")
                        .expiresAt(exp)
                        .userId(userId)
                        .walletAddress(wallet)
                        .build()
        );
    }
}
