package com.example.AudIon.controller.voice;

import com.example.AudIon.dto.voice.VoiceUploadResponse;
import com.example.AudIon.service.voice.VoiceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/voice")
@RequiredArgsConstructor
@Slf4j
public class VoiceController {

    private final VoiceService voiceService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadVoice(
            @RequestParam("file") MultipartFile file,
            @RequestParam("walletAddress") String walletAddress,
            @RequestParam(value = "duration", required = false) Float duration
    ) {
        try {
            log.info("Voice upload request - walletAddress: {}, filename: {}",
                    walletAddress, file.getOriginalFilename());

            VoiceUploadResponse response = voiceService.handleUpload(file, walletAddress, duration);

            log.info("Voice upload successful - fileId: {}, status: {}",
                    response.getFileId(), response.getStatus());

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.error("Invalid input for voice upload: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));

        } catch (Exception e) {
            log.error("Voice upload failed", e);
            return ResponseEntity.internalServerError().body(Map.of("error", "파일 업로드에 실패했습니다."));
        }
    }

    @GetMapping("/{fileId}")
    public ResponseEntity<?> getVoiceFile(@PathVariable String fileId) {
        try {
            var voiceFile = voiceService.getVoiceFile(java.util.UUID.fromString(fileId));
            return ResponseEntity.ok(voiceFile);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error retrieving voice file: {}", fileId, e);
            return ResponseEntity.internalServerError().body(Map.of("error", "파일 조회에 실패했습니다."));
        }
    }

    @GetMapping("/user/{walletAddress}")
    public ResponseEntity<?> getUserVoiceFiles(@PathVariable String walletAddress) {
        try {
            var voiceFiles = voiceService.getUserVoiceFiles(walletAddress);
            return ResponseEntity.ok(voiceFiles);

        } catch (Exception e) {
            log.error("Error retrieving user voice files: {}", walletAddress, e);
            return ResponseEntity.internalServerError().body(Map.of("error", "파일 목록 조회에 실패했습니다."));
        }
    }

    @DeleteMapping("/{fileId}")
    public ResponseEntity<?> deleteVoiceFile(
            @PathVariable String fileId,
            @RequestParam String walletAddress) {
        try {
            boolean deleted = voiceService.deleteVoiceFile(java.util.UUID.fromString(fileId), walletAddress);

            if (deleted) {
                return ResponseEntity.ok(Map.of("message", "파일이 삭제되었습니다."));
            } else {
                return ResponseEntity.internalServerError().body(Map.of("error", "파일 삭제에 실패했습니다."));
            }

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error deleting voice file: {}", fileId, e);
            return ResponseEntity.internalServerError().body(Map.of("error", "파일 삭제에 실패했습니다."));
        }
    }
}