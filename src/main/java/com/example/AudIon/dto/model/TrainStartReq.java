package com.example.AudIon.dto.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record TrainStartReq(
        @NotBlank(message = "Model ID is required")
        @Pattern(regexp = "^[0-9a-fA-F-]{36}$", message = "Invalid UUID format")
        String model_id,
        
        @NotBlank(message = "Model name is required")
        @Size(max = 100, message = "Model name must not exceed 100 characters")
        String model_name,
        
        @NotBlank(message = "Source URL is required")
        @Pattern(regexp = "^https?://.*", message = "Invalid URL format")
        String source_url
) {}
