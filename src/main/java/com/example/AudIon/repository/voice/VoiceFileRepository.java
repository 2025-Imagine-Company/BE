// repository/voice/VoiceFileRepository.java
package com.example.AudIon.repository.voice;

import com.example.AudIon.domain.voice.VoiceFile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface VoiceFileRepository extends JpaRepository<VoiceFile, UUID> {
}
