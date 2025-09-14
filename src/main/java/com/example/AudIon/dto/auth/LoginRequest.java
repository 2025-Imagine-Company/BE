package com.example.AudIon.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginRequest {
    @NotBlank
    @Pattern(regexp = "^0x[0-9a-fA-F]{40}$", message = "invalid wallet format")
    private String walletAddress;

    @NotBlank
    private String message;

    @NotBlank
    @Pattern(regexp = "^0x[0-9a-fA-F]{130}$", message = "invalid signature")
    private String signature;
}
