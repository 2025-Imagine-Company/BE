// repository/model/VoiceModelRepository.java
package com.example.AudIon.repository.model;

import com.example.AudIon.domain.model.VoiceModel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface VoiceModelRepository extends JpaRepository<VoiceModel, UUID> {
}
