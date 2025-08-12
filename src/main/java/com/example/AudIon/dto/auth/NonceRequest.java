package com.example.AudIon.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;


@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class NonceRequest {
    @NotBlank
    @Pattern(regexp = "^0x[0-9a-fA-F]{40}$", message = "invalid wallet format")
    private String walletAddress;
}



