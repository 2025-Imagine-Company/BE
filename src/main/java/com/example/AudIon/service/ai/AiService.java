package com.example.AudIon.service.ai;

import com.example.AudIon.domain.voice.VoiceFile;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class AiService {
    @Value("${ai.server.url}")
    private String aiServerUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    public void requestTrain(VoiceFile voiceFile) {
        Map<String, Object> req = Map.of(
                "voiceFileUrl", voiceFile.getFileUrl(),
                "userId", voiceFile.getUser().getId().toString()
        );
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(req, headers);

        try {
            restTemplate.postForEntity(aiServerUrl + "/train", entity, Void.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
