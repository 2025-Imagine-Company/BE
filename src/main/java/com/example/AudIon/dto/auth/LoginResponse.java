package com.example.AudIon.dto.auth;

import lombok.*;

import java.time.Instant;
import java.util.UUID;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginResponse {
    private String accessToken;
    private String tokenType;       // "Bearer"
    private Instant expiresAt;      // exp(프론트에서 자동 만료 처리 용)
    private UUID userId;
    private String walletAddress;   // normalize된 주소
}
