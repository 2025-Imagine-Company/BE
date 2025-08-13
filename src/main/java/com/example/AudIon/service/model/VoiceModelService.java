package com.example.AudIon.service.model;

import com.example.AudIon.domain.model.VoiceModel;
import com.example.AudIon.domain.user.User;
import com.example.AudIon.domain.voice.VoiceFile;
import com.example.AudIon.dto.model.ModelTrainCompleteCallbackRequest;
import com.example.AudIon.repository.model.VoiceModelRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class VoiceModelService {

    private final VoiceModelRepository voiceModelRepository;

    /**
     * 모델 생성만 담당 (PENDING, createdAt 설정)
     */
    @Transactional
    public VoiceModel createModel(User user, VoiceFile voiceFile, String modelName) {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }
        if (voiceFile == null) {
            throw new IllegalArgumentException("VoiceFile cannot be null");
        }

        // Generate default model name if not provided
        String finalModelName = StringUtils.hasText(modelName)
                ? modelName.trim()
                : "voice-model-" + System.currentTimeMillis();

        VoiceModel model = VoiceModel.builder()
                .user(user)
                .voiceFile(voiceFile)
                .modelName(finalModelName)
                .status(VoiceModel.Status.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        VoiceModel savedModel = voiceModelRepository.save(model);
        log.info("Created voice model: {} for user: {}", savedModel.getId(), user.getId());
        return savedModel;
    }

    /**
     * 학습 시작 직후 상태/잡ID 업데이트 (TRAINING)
     */
    @Transactional
    public void markTrainingStarted(UUID modelId, String jobId, String modelName) {
        if (modelId == null) {
            throw new IllegalArgumentException("Model ID cannot be null");
        }

        VoiceModel model = voiceModelRepository.findById(modelId)
                .orElseThrow(() -> new IllegalArgumentException("Model not found: " + modelId));

        // Only update if current status is PENDING
        if (model.getStatus() != VoiceModel.Status.PENDING) {
            log.warn("Attempted to mark training started for model {} with status {}",
                    modelId, model.getStatus());
            throw new IllegalStateException("Model is not in PENDING status");
        }

        model.setJobId(jobId);
        if (StringUtils.hasText(modelName)) {
            model.setModelName(modelName.trim());
        }
        model.setStatus(VoiceModel.Status.TRAINING);
        model.setUpdatedAt(LocalDateTime.now());

        voiceModelRepository.save(model);
        log.info("Marked training started for model: {} with jobId: {}", modelId, jobId);
    }

    /**
     * 학습 실패 표시
     */
    @Transactional
    public void markTrainingFailed(UUID modelId, String errorMessage) {
        if (modelId == null) {
            throw new IllegalArgumentException("Model ID cannot be null");
        }

        VoiceModel model = voiceModelRepository.findById(modelId)
                .orElseThrow(() -> new IllegalArgumentException("Model not found: " + modelId));

        model.setStatus(VoiceModel.Status.ERROR);
        model.setErrorMessage(errorMessage);
        model.setCompletedAt(LocalDateTime.now());
        model.setUpdatedAt(LocalDateTime.now());

        voiceModelRepository.save(model);
        log.error("Marked training failed for model: {} with error: {}", modelId, errorMessage);
    }

    /**
     * 콜백 반영 (DONE/ERROR + 산출물 세팅)
     */
    @Transactional
    public void applyTrainingResult(ModelTrainCompleteCallbackRequest req) {
        if (req == null || !StringUtils.hasText(req.getModelId())) {
            throw new IllegalArgumentException("Invalid callback request");
        }

        UUID id;
        try {
            id = UUID.fromString(req.getModelId());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid model ID format: " + req.getModelId(), e);
        }

        VoiceModel model = voiceModelRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Model not found: " + id));

        // Only process if current status is TRAINING
        if (model.getStatus() != VoiceModel.Status.TRAINING) {
            log.warn("Received callback for model {} with status {}, ignoring",
                    id, model.getStatus());
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        String status = req.getStatus();

        if ("DONE".equalsIgnoreCase(status) || "SUCCESS".equalsIgnoreCase(status)) {
            model.setStatus(VoiceModel.Status.DONE);

            // Set output paths if provided
            if (StringUtils.hasText(req.getModelPath())) {
                model.setModelPath(req.getModelPath().trim());
            }
            if (StringUtils.hasText(req.getPreviewUrl())) {
                model.setPreviewUrl(req.getPreviewUrl().trim());
            }

            log.info("Training completed successfully for model: {}", id);

        } else if ("ERROR".equalsIgnoreCase(status) || "FAILED".equalsIgnoreCase(status)) {
            model.setStatus(VoiceModel.Status.ERROR);

            // Set error message if provided
            String errorMsg = req.getErrorMessage();
            if (!StringUtils.hasText(errorMsg)) {
                errorMsg = "Training failed without specific error message";
            }
            model.setErrorMessage(errorMsg.trim());

            log.error("Training failed for model: {} with error: {}", id, errorMsg);

        } else {
            log.warn("Unknown status '{}' received for model: {}, treating as error", status, id);
            model.setStatus(VoiceModel.Status.ERROR);
            model.setErrorMessage("Unknown status received: " + status);
        }

        model.setCompletedAt(now);
        model.setUpdatedAt(now);
        voiceModelRepository.save(model);
    }

    /**
     * 모델 조회
     */
    @Transactional(readOnly = true)
    public VoiceModel get(UUID id) {
        if (id == null) {
            throw new IllegalArgumentException("Model ID cannot be null");
        }

        return voiceModelRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Model not found: " + id));
    }

    /**
     * 사용자별 모델 목록 조회
     */
    @Transactional(readOnly = true)
    public java.util.List<VoiceModel> getModelsByUser(UUID userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }

        return voiceModelRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    /**
     * 특정 상태의 모델들 조회 (관리용)
     */
    @Transactional(readOnly = true)
    public java.util.List<VoiceModel> getModelsByStatus(VoiceModel.Status status) {
        if (status == null) {
            throw new IllegalArgumentException("Status cannot be null");
        }

        return voiceModelRepository.findByStatusOrderByCreatedAtDesc(status);
    }
}