package com.example.AudIon.controller.model;

import com.example.AudIon.config.security.JwtAuthenticationFilter.Web3AuthenticatedUser;
import com.example.AudIon.domain.model.VoiceModel;
import com.example.AudIon.domain.user.User;
import com.example.AudIon.domain.voice.VoiceFile;
import com.example.AudIon.dto.common.PageRequest;
import com.example.AudIon.dto.common.PagedResponse;
import com.example.AudIon.dto.model.ModelTrainCompleteCallbackRequest;
import com.example.AudIon.repository.model.VoiceModelRepository;
import com.example.AudIon.repository.user.UserRepository;
import com.example.AudIon.repository.voice.VoiceFileRepository;
import com.example.AudIon.service.model.VoiceModelService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/model")
@RequiredArgsConstructor
@Slf4j
public class VoiceModelController {

    private final VoiceModelRepository voiceModelRepository;
    private final VoiceModelService voiceModelService;
    private final UserRepository userRepository;
    private final VoiceFileRepository voiceFileRepository;

    @Value("${xauth.secret}")
    private String secret;

    /**
     * 모델 생성 엔드포인트
     */
    @PostMapping
    public ResponseEntity<?> createModel(
            @RequestParam String voiceFileId,
            @RequestParam(required = false) String modelName,
            Authentication authentication
    ) {
        try {
            Web3AuthenticatedUser authUser = (Web3AuthenticatedUser) authentication.getPrincipal();
            UUID userUuid = UUID.fromString(authUser.getUserId());
            UUID voiceFileUuid = UUID.fromString(voiceFileId);

            User user = userRepository.findById(userUuid)
                    .orElseThrow(() -> new IllegalArgumentException("User not found: " + authUser.getUserId()));
            VoiceFile voiceFile = voiceFileRepository.findById(voiceFileUuid)
                    .orElseThrow(() -> new IllegalArgumentException("VoiceFile not found: " + voiceFileId));
                    
            // Security check: Verify the voice file belongs to the authenticated user
            if (!voiceFile.getWalletAddress().equalsIgnoreCase(authUser.getWalletAddress())) {
                return ResponseEntity.status(403).body(Map.of("error", "Access denied: Voice file does not belong to you"));
            }

            VoiceModel model = voiceModelService.createModel(user, voiceFile, modelName);

            return ResponseEntity.ok(Map.of(
                    "modelId", model.getId().toString(),
                    "status", model.getStatus().name(),
                    "createdAt", model.getCreatedAt()
            ));

        } catch (IllegalArgumentException e) {
            log.error("Invalid request for model creation", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error creating model", e);
            return ResponseEntity.internalServerError().body(Map.of("error", "모델 생성에 실패했습니다."));
        }
    }

    /**
     * AI 서버 학습 완료 콜백
     */
    @PostMapping("/callback")
    public ResponseEntity<?> modelTrainCallback(
            @RequestHeader("X-AUTH") String xauth,
            @Valid @RequestBody ModelTrainCompleteCallbackRequest req
    ) {
        try {
            // 보안 검증
            if (secret == null || !secret.equals(xauth)) {
                log.warn("Invalid X-AUTH header received for model: {}", req.getModelId());
                return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
            }

            // 입력 검증
            if (req.getModelId() == null || req.getModelId().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "modelId is required"));
            }

            UUID modelId = UUID.fromString(req.getModelId());
            VoiceModel model = voiceModelRepository.findById(modelId)
                    .orElseThrow(() -> new IllegalArgumentException("Model not found: " + req.getModelId()));

            // 상태 업데이트
            String status = req.getStatus();
            if ("DONE".equalsIgnoreCase(status) || "SUCCESS".equalsIgnoreCase(status)) {
                model.setStatus(VoiceModel.Status.DONE);
                model.setModelPath(req.getModelPath());
                model.setPreviewUrl(req.getPreviewUrl());
                log.info("Model training completed successfully: {}", modelId);

            } else if ("ERROR".equalsIgnoreCase(status) || "FAILED".equalsIgnoreCase(status)) {
                model.setStatus(VoiceModel.Status.ERROR);
                model.setErrorMessage(req.getErrorMessage() != null ? req.getErrorMessage() : "Training failed");
                log.error("Model training failed: {} - {}", modelId, req.getErrorMessage());

            } else {
                log.warn("Unknown status received for model {}: {}", modelId, status);
                model.setStatus(VoiceModel.Status.ERROR);
                model.setErrorMessage("Unknown status: " + status);
            }

            model.setCompletedAt(LocalDateTime.now());
            model.setUpdatedAt(LocalDateTime.now());
            voiceModelRepository.save(model);

            log.info("Processed training callback for model: {} with status: {}", modelId, status);
            return ResponseEntity.ok(Map.of("status", "success"));

        } catch (IllegalArgumentException e) {
            log.error("Model not found in callback: {}", req.getModelId(), e);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error processing training callback for model: {}", req.getModelId(), e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Callback processing failed"));
        }
    }

    /**
     * 모델 상태/정보 조회
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getModel(@PathVariable String id, Authentication authentication) {
        try {
            Web3AuthenticatedUser authUser = (Web3AuthenticatedUser) authentication.getPrincipal();
            UUID modelId = UUID.fromString(id);
            VoiceModel model = voiceModelRepository.findById(modelId)
                    .orElseThrow(() -> new IllegalArgumentException("Model not found: " + id));

            // Security check: Verify the model belongs to the authenticated user
            if (!model.getUser().getId().toString().equals(authUser.getUserId())) {
                return ResponseEntity.status(403).body(Map.of("error", "Access denied: Model does not belong to you"));
            }

            return ResponseEntity.ok(Map.of(
                    "modelId", model.getId().toString(),
                    "status", model.getStatus().name(),
                    "modelName", model.getModelName() != null ? model.getModelName() : "",
                    "modelPath", model.getModelPath() != null ? model.getModelPath() : "",
                    "previewUrl", model.getPreviewUrl() != null ? model.getPreviewUrl() : "",
                    "jobId", model.getJobId() != null ? model.getJobId() : "",
                    "createdAt", model.getCreatedAt(),
                    "completedAt", model.getCompletedAt(),
                    "errorMessage", model.getErrorMessage() != null ? model.getErrorMessage() : ""
            ));

        } catch (IllegalArgumentException e) {
            log.error("Invalid model ID: {}", id, e);
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid model ID format"));
        } catch (Exception e) {
            log.error("Error retrieving model: {}", id, e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to retrieve model"));
        }
    }

    /**
     * 사용자별 모델 목록 조회 (인증된 사용자의 모델만)
     */
    @GetMapping("/my-models")
    public ResponseEntity<?> getMyModels(Authentication authentication) {
        try {
            Web3AuthenticatedUser authUser = (Web3AuthenticatedUser) authentication.getPrincipal();
            UUID userUuid = UUID.fromString(authUser.getUserId());
            var models = voiceModelService.getModelsByUser(userUuid);
            return ResponseEntity.ok(models);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid user ID format"));
        } catch (Exception e) {
            log.error("Error retrieving user models: {}", authentication.getName(), e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to retrieve user models"));
        }
    }

    /**
     * 사용자별 모델 목록 조회 (페이지네이션)
     */
    @GetMapping("/my-models/paged")
    public ResponseEntity<?> getMyModelsPaged(
            @Valid PageRequest pageRequest,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            Authentication authentication) {
        try {
            Web3AuthenticatedUser authUser = (Web3AuthenticatedUser) authentication.getPrincipal();
            UUID userUuid = UUID.fromString(authUser.getUserId());
            
            PagedResponse<VoiceModel> result;
            
            if (status != null && !status.trim().isEmpty()) {
                // Filter by status
                VoiceModel.Status modelStatus = VoiceModel.Status.valueOf(status.toUpperCase());
                result = voiceModelService.getModelsByUserAndStatus(userUuid, modelStatus, pageRequest.toSpringPageRequest());
            } else if (search != null && !search.trim().isEmpty()) {
                // Search by model name
                result = voiceModelService.searchModelsByName(userUuid, search, pageRequest.toSpringPageRequest());
            } else {
                // Get all models
                result = voiceModelService.getModelsByUserPaged(userUuid, pageRequest.toSpringPageRequest());
            }
            
            return ResponseEntity.ok(result);

        } catch (IllegalArgumentException e) {
            log.error("Invalid parameter in paged models request: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error retrieving paged user models: {}", authentication.getName(), e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to retrieve user models"));
        }
    }
}