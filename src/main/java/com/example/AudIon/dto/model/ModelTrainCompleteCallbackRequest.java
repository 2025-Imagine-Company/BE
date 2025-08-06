package com.example.AudIon.dto.model;

import lombok.Data;

@Data
public class ModelTrainCompleteCallbackRequest {
    private String modelId;
    private String modelPath;
    private String previewUrl;
    private String status;
}
