// dto/voice/VoiceUploadResponse.java
package com.example.AudIon.dto.voice;

import lombok.Data;

@Data
public class VoiceUploadResponse {
    private String fileId;
    private String fileUrl;
    private Float duration;
}
