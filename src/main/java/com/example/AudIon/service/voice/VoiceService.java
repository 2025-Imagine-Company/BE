package com.example.AudIon.service.voice;

import com.example.AudIon.domain.user.User;
import com.example.AudIon.domain.voice.VoiceFile;
import com.example.AudIon.dto.voice.VoiceUploadResponse;
import com.example.AudIon.repository.user.UserRepository;
import com.example.AudIon.repository.voice.VoiceFileRepository;
import com.example.AudIon.service.ai.AiService;
import com.example.AudIon.service.s3.S3Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class VoiceService {

    private final S3Service s3Service;
    private final UserRepository userRepository;
    private final VoiceFileRepository voiceFileRepository;
    private final AiService aiService;

    // Constants
    private static final Pattern WALLET_ADDRESS_PATTERN = Pattern.compile("^0x[0-9a-fA-F]{40}$");
    private static final List<String> ALLOWED_AUDIO_TYPES = Arrays.asList(
            "audio/mpeg", "audio/mp3", "audio/wav", "audio/wave", "audio/x-wav",
            "audio/ogg", "audio/aac", "audio/mp4", "audio/x-m4a"
    );
    private static final long MAX_FILE_SIZE = 50 * 1024 * 1024; // 50MB
    private static final float MAX_DURATION = 300f; // 5 minutes

    @Transactional
    public VoiceUploadResponse handleUpload(MultipartFile file, String walletAddress, Float duration) {
        log.info("Starting voice file upload for wallet: {}", walletAddress);

        try {
            // 1) 입력 검증
            validateInput(file, walletAddress, duration);

            // 2) 사용자 조회 및 생성
            User user = findOrCreateUser(walletAddress);

            // 3) S3 업로드
            S3Service.S3UploadResult uploadResult = uploadToS3(file, walletAddress);

            // 4) DB 저장 (UPLOADED)
            VoiceFile voiceFile = createVoiceFile(file, user, uploadResult, duration);
            voiceFile = voiceFileRepository.save(voiceFile);

            log.info("Created voice file record: {} for user: {}", voiceFile.getId(), user.getId());

            // 5) AI 학습 즉시 시작
            handleAiTraining(voiceFile);

            // 6) 응답 생성
            return createResponse(voiceFile);

        } catch (Exception e) {
            log.error("Failed to handle voice upload for wallet: {}", walletAddress, e);
            throw e;
        }
    }

    /**
     * AI 학습 즉시 처리 (업로드와 함께)
     */
    private void handleAiTraining(VoiceFile voiceFile) {
        try {
            log.info("Starting immediate AI training for voice file: {}", voiceFile.getId());

            String jobId = aiService.requestTrain(voiceFile);

            voiceFile.setStatus(VoiceFile.Status.TRAINING);
            voiceFile.setJobId(jobId);
            voiceFileRepository.save(voiceFile);

            log.info("AI training started for voice file: {} with jobId: {}", voiceFile.getId(), jobId);

        } catch (Exception e) {
            log.error("AI training request failed for voice file: {}", voiceFile.getId(), e);

            voiceFile.setStatus(VoiceFile.Status.FAILED);
            voiceFile.setErrorMessage("AI 학습 요청 실패: " + e.getMessage());
            voiceFileRepository.save(voiceFile);

            // 학습 실패해도 파일 업로드는 성공으로 처리하되, 상태만 FAILED로 변경
            // 전체 업로드를 실패로 처리하고 싶다면 아래 주석을 해제
            // throw new RuntimeException("AI 학습 요청에 실패했습니다", e);
        }
    }

    /**
     * 음성 파일 조회 (보안 검사 없음 - 내부 사용)
     */
    @Transactional(readOnly = true)
    public VoiceFile getVoiceFile(UUID id) {
        if (id == null) {
            throw new IllegalArgumentException("Voice file ID cannot be null");
        }

        return voiceFileRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Voice file not found: " + id));
    }

    /**
     * 음성 파일 조회 (보안 검사 포함 - 컨트롤러에서 사용)
     */
    @Transactional(readOnly = true)
    public VoiceFile getVoiceFile(UUID id, String walletAddress) {
        VoiceFile voiceFile = getVoiceFile(id);
        
        // Security check: Verify ownership using direct walletAddress field
        if (!voiceFile.getWalletAddress().equalsIgnoreCase(walletAddress)) {
            throw new IllegalArgumentException("Access denied: Voice file does not belong to you");
        }
        
        return voiceFile;
    }

    /**
     * 사용자별 음성 파일 목록 조회
     */
    @Transactional(readOnly = true)
    public List<VoiceFile> getUserVoiceFiles(String walletAddress) {
        if (!StringUtils.hasText(walletAddress)) {
            throw new IllegalArgumentException("Wallet address cannot be null or empty");
        }

        return voiceFileRepository.findByUserWalletAddressOrderByUploadedAtDesc(walletAddress);
    }

    /**
     * 음성 파일 삭제
     */
    @Transactional
    public boolean deleteVoiceFile(UUID id, String walletAddress) {
        VoiceFile voiceFile = getVoiceFile(id);

        // 권한 확인 - Direct wallet address comparison for better performance
        if (!voiceFile.getWalletAddress().equalsIgnoreCase(walletAddress)) {
            throw new IllegalArgumentException("Access denied: file belongs to different user");
        }

        // 학습 중인 파일은 삭제할 수 없음
        if (voiceFile.getStatus() == VoiceFile.Status.TRAINING) {
            throw new IllegalStateException("Cannot delete file while training is in progress");
        }

        try {
            // S3에서 파일 삭제
            if (StringUtils.hasText(voiceFile.getS3Key())) {
                boolean deleted = s3Service.deleteFile(voiceFile.getS3Key());
                if (!deleted) {
                    log.warn("Failed to delete file from S3: {}", voiceFile.getS3Key());
                }
            }

            // DB에서 삭제
            voiceFileRepository.delete(voiceFile);

            log.info("Deleted voice file: {} for user: {}", id, walletAddress);
            return true;

        } catch (Exception e) {
            log.error("Failed to delete voice file: {}", id, e);
            return false;
        }
    }

    // Private helper methods

    private void validateInput(MultipartFile file, String walletAddress, Float duration) {
        // File validation
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("파일이 비어 있습니다.");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("파일 크기가 너무 큽니다. 최대 50MB까지 허용됩니다.");
        }

        // Wallet address validation
        if (!StringUtils.hasText(walletAddress) || !WALLET_ADDRESS_PATTERN.matcher(walletAddress).matches()) {
            throw new IllegalArgumentException("지갑 주소 형식이 올바르지 않습니다.");
        }

        // Content type validation
        String contentType = file.getContentType();
        if (!StringUtils.hasText(contentType) || !ALLOWED_AUDIO_TYPES.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException("지원되지 않는 오디오 형식입니다. 허용된 형식: " + ALLOWED_AUDIO_TYPES);
        }

        // Duration validation
        if (duration != null && (duration <= 0 || duration > MAX_DURATION)) {
            throw new IllegalArgumentException("오디오 길이는 0초보다 크고 " + (MAX_DURATION/60) + "분 이하여야 합니다.");
        }
    }

    private User findOrCreateUser(String walletAddress) {
        return userRepository.findByWalletAddress(walletAddress)
                .orElseGet(() -> {
                    log.info("Creating new user for wallet address: {}", walletAddress);
                    User newUser = User.builder()
                            .walletAddress(walletAddress)
                            .createdAt(LocalDateTime.now())
                            .build();
                    User savedUser = userRepository.save(newUser);
                    log.info("Created new user with ID: {} for wallet: {}", savedUser.getId(), walletAddress);
                    return savedUser;
                });
    }

    private S3Service.S3UploadResult uploadToS3(MultipartFile file, String walletAddress) {
        try {
            // 개선된 S3Service 사용 - public 업로드, presigned URL 없음
            return s3Service.uploadVoiceFile(file, walletAddress, true, 0);
        } catch (Exception e) {
            log.error("S3 upload failed for wallet: {}", walletAddress, e);
            throw new RuntimeException("파일 업로드에 실패했습니다: " + e.getMessage(), e);
        }
    }

    private VoiceFile createVoiceFile(MultipartFile file, User user, S3Service.S3UploadResult uploadResult, Float duration) {
        return VoiceFile.builder()
                .user(user)
                .walletAddress(user.getWalletAddress()) // Set wallet address directly
                .fileUrl(uploadResult.publicUrl() != null ? uploadResult.publicUrl() : uploadResult.s3Url())
                .s3Key(uploadResult.key()) // S3에서의 실제 키 저장
                .originalFilename(sanitizeFilename(file.getOriginalFilename()))
                .contentType(file.getContentType())
                .fileSize(file.getSize())
                .duration(duration)
                .uploadedAt(LocalDateTime.now())
                .status(VoiceFile.Status.UPLOADED)
                .audioFormat(getFileExtension(file.getOriginalFilename()))
                .build();
    }

    private VoiceUploadResponse createResponse(VoiceFile voiceFile) {
        return VoiceUploadResponse.builder()
                .fileId(voiceFile.getId().toString())
                .fileUrl(voiceFile.getFileUrl())
                .originalFilename(voiceFile.getOriginalFilename())
                .contentType(voiceFile.getContentType())
                .fileSize(voiceFile.getFileSize())
                .duration(voiceFile.getDuration())
                .status(voiceFile.getStatus().name())
                .uploadedAt(voiceFile.getUploadedAt())
                .jobId(voiceFile.getJobId())
                .errorMessage(voiceFile.getErrorMessage())
                .build();
    }

    private String sanitizeFilename(String filename) {
        if (!StringUtils.hasText(filename)) {
            return "unknown_file";
        }

        // 파일 확장자 보존하면서 안전한 문자만 허용
        String nameWithoutExt = filename.contains(".")
                ? filename.substring(0, filename.lastIndexOf("."))
                : filename;
        String extension = filename.contains(".")
                ? filename.substring(filename.lastIndexOf("."))
                : "";

        String sanitizedName = nameWithoutExt.replaceAll("[^a-zA-Z0-9._\\-가-힣]", "_");
        return sanitizedName + extension;
    }

    private String getFileExtension(String filename) {
        if (!StringUtils.hasText(filename) || !filename.contains(".")) {
            return null;
        }

        return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
    }
}