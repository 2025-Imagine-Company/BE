package com.example.AudIon.service.voice;

import com.example.AudIon.domain.user.User;
import com.example.AudIon.domain.voice.VoiceFile;
import com.example.AudIon.dto.voice.VoiceUploadResponse;
import com.example.AudIon.repository.user.UserRepository;
import com.example.AudIon.repository.voice.VoiceFileRepository;
import com.example.AudIon.service.ai.AiService;
import com.example.AudIon.service.s3.S3Service;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class VoiceService {

    private final S3Service s3Service;
    private final UserRepository userRepository;
    private final VoiceFileRepository voiceFileRepository;
    private final AiService aiService;

    public VoiceUploadResponse handleUpload(MultipartFile file, String walletAddress, Float duration) {
        // 1) 입력 검증
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("파일이 비어 있습니다.");
        }
        if (walletAddress == null || !walletAddress.matches("^0x[0-9a-fA-F]{40}$")) {
            throw new IllegalArgumentException("지갑 주소 형식이 올바르지 않습니다.");
        }

        // (선택) 콘텐츠 타입 허용 리스트 체크
        var ct = file.getContentType();
        if (ct == null || !(ct.startsWith("audio/"))) {
            throw new IllegalArgumentException("오디오 파일만 업로드할 수 있습니다. contentType=" + ct);
        }

        // 2) 사용자 조회
        User user = userRepository.findByWalletAddress(walletAddress)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // 3) S3 업로드 (IOException 처리)
        String fileUrl;
        try {
            fileUrl = s3Service.uploadVoiceFile(file, walletAddress);
        } catch (IOException e) {
            throw new IllegalStateException("S3 파일 업로드 실패", e);
        }

        // 4) DB 저장 (UPLOADED)
        VoiceFile voiceFile = VoiceFile.builder()
                .user(user)
                .fileUrl(fileUrl)
                .originalFilename(safeName(file.getOriginalFilename()))
                .contentType(ct)
                .size(file.getSize())
                .duration(duration)
                .uploadedAt(LocalDateTime.now())
                .status(VoiceFile.Status.UPLOADED)
                .build();

        voiceFileRepository.save(voiceFile);

        // 5) AI 학습 요청 → 상태 업데이트
        try {
            String jobId = aiService.requestTrain(voiceFile);
            voiceFile.setStatus(VoiceFile.Status.TRAINING);
            voiceFile.setJobId(jobId);
            voiceFileRepository.save(voiceFile);
        } catch (Exception e) {
            voiceFile.setStatus(VoiceFile.Status.FAILED);
            voiceFile.setErrorMsg(e.getMessage());
            voiceFileRepository.save(voiceFile);
            throw e;
        }

        // 6) 응답
        var resp = new VoiceUploadResponse();
        resp.setFileId(voiceFile.getId().toString());
        resp.setFileUrl(fileUrl);
        resp.setDuration(duration);
        resp.setStatus(voiceFile.getStatus().name());
        return resp;
    }

    private String safeName(String name) {
        if (name == null) return "unknown";
        return name.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
