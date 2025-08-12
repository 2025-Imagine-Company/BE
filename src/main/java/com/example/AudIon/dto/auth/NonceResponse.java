package com.example.AudIon.dto.auth;

import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NonceResponse {
    private String walletAddress;   // normalize된 주소(소문자)
    private String nonce;           // 서버가 발급한 nonce
    private String message;         // "AudIon Login:\nnonce=<...>"
    private Instant expiresAt;      // TTL(예: now + 5m)
}
