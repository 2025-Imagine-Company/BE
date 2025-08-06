package com.example.AudIon.controller.model;

import com.example.AudIon.domain.model.VoiceModel;
import com.example.AudIon.dto.model.ModelTrainCompleteCallbackRequest;
import com.example.AudIon.repository.model.VoiceModelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/model")
@RequiredArgsConstructor
public class VoiceModelController {
    private final VoiceModelRepository voiceModelRepository;

    // AI 서버 학습 완료 콜백 (POST /model/callback)
    @PostMapping("/callback")
    public ResponseEntity<Void> modelTrainCallback(@RequestBody ModelTrainCompleteCallbackRequest req) {
        VoiceModel model = voiceModelRepository.findById(UUID.fromString(req.getModelId()))
                .orElseThrow(() -> new IllegalArgumentException("Model not found"));

        model.setModelPath(req.getModelPath());
        model.setPreviewUrl(req.getPreviewUrl());
        model.setStatus(VoiceModel.Status.valueOf(req.getStatus()));
        model.setCompletedAt(LocalDateTime.now());
        voiceModelRepository.save(model);

        return ResponseEntity.ok().build();
    }

    // 모델 상태/정보 조회 (GET /model/{id})
    @GetMapping("/{id}")
    public ResponseEntity<VoiceModel> getModel(@PathVariable String id) {
        VoiceModel model = voiceModelRepository.findById(UUID.fromString(id))
                .orElseThrow(() -> new IllegalArgumentException("Model not found"));
        return ResponseEntity.ok(model);
    }
}
