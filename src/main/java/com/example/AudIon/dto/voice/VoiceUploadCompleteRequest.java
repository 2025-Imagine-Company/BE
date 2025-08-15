package com.example.AudIon.dto.voice;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class VoiceUploadCompleteRequest {
    
    @NotBlank(message = "File URL is required")
    @Pattern(regexp = "^https?://.*", message = "Invalid URL format")
    private String fileUrl;
    
    @NotBlank(message = "Wallet address is required")
    @Pattern(regexp = "^0x[0-9a-fA-F]{40}$", message = "Invalid Ethereum wallet address format")
    private String walletAddress;
    
    @Positive(message = "Duration must be positive")
    @DecimalMax(value = "3600.0", message = "Duration cannot exceed 3600 seconds (1 hour)")
    private Float duration;
}
