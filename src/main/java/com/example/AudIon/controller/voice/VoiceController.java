package com.example.AudIon.controller.voice;

import com.example.AudIon.dto.voice.VoiceUploadResponse;
import com.example.AudIon.service.voice.VoiceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;

@RestController
@RequestMapping("/voice")
@RequiredArgsConstructor
public class VoiceController {

    private final VoiceService voiceService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<VoiceUploadResponse> uploadVoice(
            @RequestPart("file") MultipartFile file,
            @RequestParam("walletAddress") String walletAddress,
            @RequestParam(value = "duration", required = false) Float duration
    ) {
        VoiceUploadResponse resp = voiceService.handleUpload(file, walletAddress, duration);
        return ResponseEntity.created(URI.create("/voice/files/" + resp.getFileId())).body(resp);
    }
}
