package com.example.AudIon.service.model;

import com.example.AudIon.domain.model.VoiceModel;
import com.example.AudIon.domain.user.User;
import com.example.AudIon.domain.voice.VoiceFile;
import com.example.AudIon.dto.common.PagedResponse;
import com.example.AudIon.repository.model.VoiceModelRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class VoiceModelService {

    private final VoiceModelRepository voiceModelRepository;

    @Transactional
    public VoiceModel createModel(User user, VoiceFile voiceFile, String modelName) {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }
        if (voiceFile == null) {
            throw new IllegalArgumentException("VoiceFile cannot be null");
        }

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

    @Transactional(readOnly = true)
    public VoiceModel get(UUID id) {
        if (id == null) {
            throw new IllegalArgumentException("Model ID cannot be null");
        }

        return voiceModelRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Model not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<VoiceModel> getModelsByUser(UUID userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }

        return voiceModelRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    /**
     * 사용자별 모델 목록 조회 (페이지네이션)
     */
    @Transactional(readOnly = true)
    public PagedResponse<VoiceModel> getModelsByUserPaged(UUID userId, Pageable pageable) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }

        Page<VoiceModel> page = voiceModelRepository.findByUserId(userId, pageable);
        return PagedResponse.of(page.getContent(), page);
    }

    /**
     * 지갑 주소로 모델 목록 조회 (페이지네이션)
     */
    @Transactional(readOnly = true)
    public PagedResponse<VoiceModel> getModelsByWalletPaged(String walletAddress, Pageable pageable) {
        if (!StringUtils.hasText(walletAddress)) {
            throw new IllegalArgumentException("Wallet address cannot be null or empty");
        }

        Page<VoiceModel> page = voiceModelRepository.findByUserWalletAddress(walletAddress, pageable);
        return PagedResponse.of(page.getContent(), page);
    }

    /**
     * 사용자별 특정 상태의 모델들 조회 (페이지네이션)
     */
    @Transactional(readOnly = true)
    public PagedResponse<VoiceModel> getModelsByUserAndStatus(UUID userId, VoiceModel.Status status, Pageable pageable) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        if (status == null) {
            throw new IllegalArgumentException("Status cannot be null");
        }

        Page<VoiceModel> page = voiceModelRepository.findByUserIdAndStatus(userId, status, pageable);
        return PagedResponse.of(page.getContent(), page);
    }

    /**
     * 모델명으로 검색 (페이지네이션)
     */
    @Transactional(readOnly = true)
    public PagedResponse<VoiceModel> searchModelsByName(UUID userId, String modelName, Pageable pageable) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }

        Page<VoiceModel> page = voiceModelRepository.findByUserIdAndModelNameContaining(
                userId, modelName != null ? modelName : "", pageable);
        return PagedResponse.of(page.getContent(), page);
    }

    @Transactional(readOnly = true)
    public List<VoiceModel> getModelsByStatus(VoiceModel.Status status) {
        if (status == null) {
            throw new IllegalArgumentException("Status cannot be null");
        }

        return voiceModelRepository.findByStatusOrderByCreatedAtDesc(status);
    }
}