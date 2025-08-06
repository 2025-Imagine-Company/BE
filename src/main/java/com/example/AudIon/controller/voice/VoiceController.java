// controller/voice/VoiceController.java
package com.example.AudIon.controller.voice;

import com.audion.domain.user.User;
import com.audion.domain.voice.VoiceFile;
import com.audion.dto.voice.VoiceUploadResponse;
import com.audion.repository.user.UserRepository;
import com.audion.repository.voice.VoiceFileRepository;
import com.audion.service.ai.AiService;
import com.audion.service.s3.S3Service;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/voice")
@RequiredArgsConstructor
public class VoiceController {
    private final S3Service s3Service;
    private final UserRepository userRepository;
    private final VoiceFileRepository voiceFileRepository;
    private final AiService aiService;

    @PostMapping("/upload")
    public ResponseEntity<VoiceUploadResponse> uploadVoice(
            @RequestParam("file") MultipartFile file,
            @RequestParam("walletAddress") String walletAddress,
            @RequestParam(value = "duration", required = false) Float duration
    ) throws IOException {
        User user = userRepository.findByWalletAddress(walletAddress)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        // 1. S3에 파일 업로드
        String fileUrl = s3Service.uploadVoiceFile(file, walletAddress);
        // 2. DB 저장
        VoiceFile voiceFile = VoiceFile.builder()
                .user(user)
                .fileUrl(fileUrl)
                .duration(duration)
                .uploadedAt(LocalDateTime.now())
                .build();
        voiceFileRepository.save(voiceFile);
        // 3. AI 서버에 학습 요청
        aiService.requestTrain(voiceFile);
        // 4. 응답
        VoiceUploadResponse response = new VoiceUploadResponse();
        response.setFileId(voiceFile.getId().toString());
        response.setFileUrl(fileUrl);
        response.setDuration(duration);
        return ResponseEntity.ok(response);
    }
}
