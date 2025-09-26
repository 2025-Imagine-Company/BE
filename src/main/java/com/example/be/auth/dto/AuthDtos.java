package com.example.be.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
class NonceRequest {
    @NotBlank
    @Size(max = 64)
    private String address;
}

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
class NonceResponse {
    private String nonce;
    private long expiresIn;
}

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
class VerifyRequest {
    @NotBlank
    @Size(max = 64)
    private String address;

    @NotBlank
    private String signature;
}

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
class TokenResponse {
    private String accessToken;
    private String refreshToken;
}


