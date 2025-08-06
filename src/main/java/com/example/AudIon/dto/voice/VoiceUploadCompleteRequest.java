// dto/voice/VoiceUploadCompleteRequest.java
package com.example.AudIon.dto.voice;

import lombok.Data;

@Data
public class VoiceUploadCompleteRequest {
    private String fileUrl;
    private String walletAddress;
    private Float duration;
}
