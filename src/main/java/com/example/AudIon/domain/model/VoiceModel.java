package com.example.AudIon.domain.model;

import com.example.AudIon.domain.user.User;
import com.example.AudIon.domain.voice.VoiceFile;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
@Table(name = "voice_models")
public class VoiceModel {
    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @OneToOne
    @JoinColumn(name = "voice_file_id")
    private VoiceFile voiceFile;

    @Column(nullable = false)
    private String modelPath; // 학습된 모델 파일 위치

    @Enumerated(EnumType.STRING)
    private Status status; // pending, training, done, error

    private String previewUrl;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;

    public enum Status {
        PENDING, TRAINING, DONE, ERROR
    }
}
