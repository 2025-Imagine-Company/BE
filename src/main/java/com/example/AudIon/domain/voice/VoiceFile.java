package com.example.AudIon.domain.voice;

import com.example.AudIon.domain.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter @Setter
@Builder
@NoArgsConstructor @AllArgsConstructor
public class VoiceFile {

    public enum Status { UPLOADED, TRAINING, READY, FAILED }

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    private User user;

    private String fileUrl;

    // 신규 필드
    private String originalFilename;
    private String contentType;
    private Long size;

    private Float duration;
    private LocalDateTime uploadedAt;

    // 학습 추적
    @Enumerated(EnumType.STRING)
    private Status status;

    private String jobId;
    @Column(length = 2000)
    private String errorMsg;
}
