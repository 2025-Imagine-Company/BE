package com.example.AudIon.dto.auth;

import lombok.*;

import java.time.Instant;
import java.util.UUID;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MeResponse {
    private UUID userId;
    private String walletAddress;
    private Instant tokenExpiresAt;
}
