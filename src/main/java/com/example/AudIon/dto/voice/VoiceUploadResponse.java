package com.example.AudIon.dto.voice;

import lombok.Getter; import lombok.Setter;

@Getter @Setter
public class VoiceUploadResponse {
    private String fileId;
    private String fileUrl;
    private Float duration;
    private String status; // UPLOADED/TRAINING/READY/FAILED
}
