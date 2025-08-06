package com.example.AudIon.domain.voice;

import com.example.AudIon.domain.user.User;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
@Table(name = "voice_files")
public class VoiceFile {
    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false)
    private String fileUrl;

    private Float duration;

    private LocalDateTime uploadedAt;
}
