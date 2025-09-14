package com.example.AudIon.service.ai;

import com.example.AudIon.domain.voice.VoiceFile;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiService {

    @Value("${ai.server.url}")
    private String aiServerUrl;

    @Value("${xauth.secret}")
    private String authSecret;

    private final RestTemplate restTemplate;

    /**
     * AI 서버에 학습 요청을 보내고 jobId를 반환
     */
    public String requestTrain(VoiceFile voiceFile) {
        if (voiceFile == null) {
            throw new IllegalArgumentException("VoiceFile cannot be null");
        }

        if (voiceFile.getFileUrl() == null || voiceFile.getFileUrl().trim().isEmpty()) {
            throw new IllegalArgumentException("VoiceFile must have a valid file URL");
        }

        try {
            Map<String, Object> requestBody = Map.of(
                    "voiceFileId", voiceFile.getId().toString(),
                    "voiceFileUrl", voiceFile.getFileUrl(),
                    "userId", voiceFile.getUser().getId().toString(),
                    "walletAddress", voiceFile.getUser().getWalletAddress(),
                    "originalFilename", voiceFile.getOriginalFilename() != null ? voiceFile.getOriginalFilename() : "unknown",
                    "duration", voiceFile.getDuration() != null ? voiceFile.getDuration() : 0.0f
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-AUTH", authSecret); // AI 서버 인증

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            log.info("Sending training request to AI server for voiceFile: {}", voiceFile.getId());

            String trainUrl = aiServerUrl + "/train";
            ResponseEntity<?> rawResponse = restTemplate.postForEntity(trainUrl, entity, Map.class);
            @SuppressWarnings("unchecked")
            ResponseEntity<Map<String, Object>> response = (ResponseEntity<Map<String, Object>>) rawResponse;

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                String jobId = (String) response.getBody().get("jobId");

                if (jobId == null || jobId.trim().isEmpty()) {
                    log.warn("AI server returned success but no jobId for voiceFile: {}", voiceFile.getId());
                    return "no-job-id-" + System.currentTimeMillis();
                }

                log.info("AI training request successful - voiceFile: {}, jobId: {}", voiceFile.getId(), jobId);
                return jobId;

            } else {
                throw new RuntimeException("AI server returned non-success status: " + response.getStatusCode());
            }

        } catch (RestClientException e) {
            log.error("Failed to communicate with AI server for voiceFile: {}", voiceFile.getId(), e);
            throw new RuntimeException("AI 서버 통신 실패: " + e.getMessage(), e);

        } catch (Exception e) {
            log.error("Unexpected error during AI training request for voiceFile: {}", voiceFile.getId(), e);
            throw new RuntimeException("AI 학습 요청 처리 중 오류 발생: " + e.getMessage(), e);
        }
    }

    /**
     * AI 서버에서 학습 상태 조회
     */
    public Map<String, Object> getTrainingStatus(String jobId) {
        if (jobId == null || jobId.trim().isEmpty()) {
            throw new IllegalArgumentException("JobId cannot be null or empty");
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-AUTH", authSecret);

            HttpEntity<?> entity = new HttpEntity<>(headers);
            String statusUrl = aiServerUrl + "/train/status/" + jobId;

            ResponseEntity<?> rawResponse = restTemplate.exchange(statusUrl, HttpMethod.GET, entity, Map.class);
            @SuppressWarnings("unchecked")
            ResponseEntity<Map<String, Object>> response = (ResponseEntity<Map<String, Object>>) rawResponse;

            if (response.getStatusCode().is2xxSuccessful()) {
                return response.getBody();
            } else {
                throw new RuntimeException("Failed to get training status: " + response.getStatusCode());
            }

        } catch (RestClientException e) {
            log.error("Failed to get training status for jobId: {}", jobId, e);
            throw new RuntimeException("학습 상태 조회 실패: " + e.getMessage(), e);
        }
    }

    /**
     * AI 서버에 학습 중단 요청
     */
    public boolean cancelTraining(String jobId) {
        if (jobId == null || jobId.trim().isEmpty()) {
            return false;
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-AUTH", authSecret);

            HttpEntity<?> entity = new HttpEntity<>(headers);
            String cancelUrl = aiServerUrl + "/train/cancel/" + jobId;

            ResponseEntity<?> rawResponse = restTemplate.exchange(cancelUrl, HttpMethod.POST, entity, Map.class);
            @SuppressWarnings("unchecked")
            ResponseEntity<Map<String, Object>> response = (ResponseEntity<Map<String, Object>>) rawResponse;

            return response.getStatusCode().is2xxSuccessful();

        } catch (Exception e) {
            log.error("Failed to cancel training for jobId: {}", jobId, e);
            return false;
        }
    }
}