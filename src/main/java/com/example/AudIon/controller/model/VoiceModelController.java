package com.example.AudIon.controller.model;

import com.example.AudIon.domain.model.VoiceModel;
import com.example.AudIon.domain.user.User;
import com.example.AudIon.domain.voice.VoiceFile;
import com.example.AudIon.dto.model.ModelTrainCompleteCallbackRequest;
import com.example.AudIon.repository.model.VoiceModelRepository;
import com.example.AudIon.repository.user.UserRepository;
import com.example.AudIon.repository.voice.VoiceFileRepository;
import com.example.AudIon.service.model.VoiceModelService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/model")
@RequiredArgsConstructor
@Slf4j
public class VoiceModelController {

    private final VoiceModelService voiceModelService;
    private final VoiceModelRepository voiceModelRepository;
    private final UserRepository userRepository;
    private final VoiceFileRepository voiceFileRepository;
    private final RestTemplate aiRestTemplate;

    @Value("${ai.server.url}")
    private String aiBaseUrl;

    @Value("${xauth.secret}")
    private String secret;

    /**
     * 모델 생성 + AI 학습 시작 트리거
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createAndStart(
            @RequestParam String userId,
            @RequestParam String voiceFileId,
            @RequestParam(required = false) String modelName
    ) {
        try {
            // Input validation
            if (userId == null || userId.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "userId is required"));
            }
            if (voiceFileId == null || voiceFileId.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "voiceFileId is required"));
            }

            UUID userUuid;
            UUID voiceFileUuid;
            try {
                userUuid = UUID.fromString(userId);
                voiceFileUuid = UUID.fromString(voiceFileId);
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid UUID format"));
            }

            User user = userRepository.findById(userUuid)
                    .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
            VoiceFile voiceFile = voiceFileRepository.findById(voiceFileUuid)
                    .orElseThrow(() -> new IllegalArgumentException("VoiceFile not found: " + voiceFileId));

            // Validate voice file has valid S3 URL
            if (voiceFile.getS3Url() == null || voiceFile.getS3Url().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "VoiceFile has no valid S3 URL"));
            }

            // 1) DB에 모델 생성 (PENDING)
            VoiceModel model = voiceModelService.createModel(user, voiceFile, modelName);
            log.info("Created voice model: {}", model.getId());

            // 2) AI 서버에 학습 시작 요청
            Map<String, Object> startReq = new HashMap<>();
            startReq.put("model_id", model.getId().toString());
            startReq.put("model_name", model.getModelName() != null ? model.getModelName() : ("voice-" + model.getId()));
            startReq.put("source_urls", List.of(voiceFile.getS3Url()));

            String startUrl = aiBaseUrl + "/ai/train/start";

            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> startResp = aiRestTemplate.postForObject(startUrl, startReq, Map.class);

                if (startResp == null) {
                    log.error("AI server returned null response for model: {}", model.getId());
                    voiceModelService.markTrainingFailed(model.getId(), "AI server returned null response");
                    return ResponseEntity.internalServerError().body(Map.of("error", "Failed to start training"));
                }

                // 3) 응답 반영 (TRAINING + jobId)
                String jobId = (String) startResp.get("job_id");
                if (jobId == null || jobId.trim().isEmpty()) {
                    log.warn("AI server didn't return job_id for model: {}", model.getId());
                }

                voiceModelService.markTrainingStarted(model.getId(), jobId, model.getModelName());
                log.info("Started training for model: {} with jobId: {}", model.getId(), jobId);

                // 4) 응답
                return ResponseEntity.ok(Map.of(
                        "modelId", model.getId().toString(),
                        "status", VoiceModel.Status.TRAINING.name(),
                        "jobId", jobId != null ? jobId : ""
                ));

            } catch (RestClientException e) {
                log.error("Failed to communicate with AI server for model: {}", model.getId(), e);
                voiceModelService.markTrainingFailed(model.getId(), "AI server communication failed: " + e.getMessage());
                return ResponseEntity.internalServerError().body(Map.of("error", "Failed to start training"));
            }

        } catch (IllegalArgumentException e) {
            log.error("Invalid request parameters", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error during model creation", e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Internal server error"));
        }
    }

    /**
     * AI 서버 학습 완료 콜백
     */
    @PostMapping("/callback")
    public ResponseEntity<Map<String, String>> modelTrainCallback(
            @RequestHeader("X-AUTH") String xauth,
            @Valid @RequestBody ModelTrainCompleteCallbackRequest req
    ) {
        try {
            // Security validation
            if (secret == null || !secret.equals(xauth)) {
                log.warn("Invalid X-AUTH header received: {}", xauth);
                return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
            }

            // Input validation
            if (req.getModelId() == null || req.getModelId().trim().isEmpty()) {
                log.error("Callback received with empty modelId");
                return ResponseEntity.badRequest().body(Map.of("error", "modelId is required"));
            }

            try {
                UUID.fromString(req.getModelId());
            } catch (IllegalArgumentException e) {
                log.error("Callback received with invalid modelId format: {}", req.getModelId());
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid modelId format"));
            }

            log.info("Processing training callback for model: {} with status: {}", req.getModelId(), req.getStatus());
            voiceModelService.applyTrainingResult(req);

            return ResponseEntity.ok(Map.of("status", "success"));

        } catch (IllegalArgumentException e) {
            log.error("Model not found in callback: {}", req.getModelId(), e);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error processing training callback for model: {}", req.getModelId(), e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Internal server error"));
        }
    }

    /**
     * 모델 상태/정보 조회
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getModel(@PathVariable String id) {
        try {
            UUID modelId;
            try {
                modelId = UUID.fromString(id);
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid UUID format"));
            }

            VoiceModel model = voiceModelService.get(modelId);
            return ResponseEntity.ok(Map.of(
                    "modelId", model.getId().toString(),
                    "status", model.getStatus().name(),
                    "modelName", model.getModelName() != null ? model.getModelName() : "",
                    "jobId", model.getJobId() != null ? model.getJobId() : "",
                    "createdAt", model.getCreatedAt(),
                    "completedAt", model.getCompletedAt()
            ));

        } catch (IllegalArgumentException e) {
            log.error("Model not found: {}", id, e);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error retrieving model: {}", id, e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Internal server error"));
        }
    }
}