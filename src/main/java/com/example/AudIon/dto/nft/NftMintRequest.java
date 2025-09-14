package com.example.AudIon.dto.nft;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class NftMintRequest {
    
    @NotBlank(message = "Model ID is required")
    @Pattern(regexp = "^[0-9a-fA-F-]{36}$", message = "Invalid UUID format")
    private String modelId;
    
    // Note: ownerWallet removed - will be taken from JWT authentication
}
