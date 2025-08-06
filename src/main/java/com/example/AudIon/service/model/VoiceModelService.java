package com.example.AudIon.service.model;

import com.example.AudIon.domain.model.VoiceModel;
import com.example.AudIon.domain.user.User;
import com.example.AudIon.domain.voice.VoiceFile;
import com.example.AudIon.repository.model.VoiceModelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class VoiceModelService {
    private final VoiceModelRepository voiceModelRepository;

    public VoiceModel createModel(User user, VoiceFile voiceFile) {
        VoiceModel model = VoiceModel.builder()
                .user(user)
                .voiceFile(voiceFile)
                .status(VoiceModel.Status.PENDING)
                .createdAt(LocalDateTime.now())
                .build();
        return voiceModelRepository.save(model);
    }
}
