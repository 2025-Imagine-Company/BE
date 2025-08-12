package com.example.AudIon.service.ai;

import com.example.AudIon.domain.voice.VoiceFile;
import lombok.Getter; import lombok.Setter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AiService {

    @Value("${ai.server.url}")
    private String aiServerUrl;

    @Value("${ai.shared.secret:}")
    private String sharedSecret;

    private final RestTemplate restTemplate;

    @Getter @Setter
    public static class TrainResponse {
        private String jobId;
        private String status;
        private String message;
    }

    public String requestTrain(VoiceFile voiceFile) {
        Map<String, Object> req = Map.of(
                "voiceFileUrl", voiceFile.getFileUrl(),
                "userId", voiceFile.getUser().getId().toString(),
                "signature", sign(voiceFile.getFileUrl())
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (!sharedSecret.isBlank()) {
            headers.setBearerAuth(sharedSecret);
        }
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(req, headers);

        ResponseEntity<TrainResponse> resp;
        try {
            resp = restTemplate.postForEntity(aiServerUrl + "/train", entity, TrainResponse.class);
        } catch (ResourceAccessException e) {
            throw new IllegalStateException("AI server timeout: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new IllegalStateException("AI server error: " + e.getMessage(), e);
        }

        if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null || resp.getBody().getJobId() == null) {
            throw new IllegalStateException("Invalid AI response: " + resp.getStatusCode());
        }
        return resp.getBody().getJobId();
    }

    private String sign(String data) {
        // 간단 placeholder (운영에선 HMAC-SHA256 권장)
        return org.springframework.util.DigestUtils.md5DigestAsHex((data + sharedSecret).getBytes(StandardCharsets.UTF_8));
    }
}
