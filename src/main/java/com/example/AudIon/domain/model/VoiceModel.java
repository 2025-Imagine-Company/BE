package com.example.AudIon.domain.model;

import com.example.AudIon.domain.user.User;
import com.example.AudIon.domain.voice.VoiceFile;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
@Table(name = "voice_models", indexes = {
        @Index(name = "idx_voice_model_user_id", columnList = "user_id"),
        @Index(name = "idx_voice_model_status", columnList = "status"),
        @Index(name = "idx_voice_model_created_at", columnList = "created_at"),
        @Index(name = "idx_voice_model_job_id", columnList = "job_id")
})
public class VoiceModel {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @NotNull
    private User user;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "voice_file_id", nullable = false)
    @NotNull
    private VoiceFile voiceFile;

    @Column(name = "model_name", length = 255)
    @Size(max = 255)
    private String modelName;

    @Column(name = "model_path", length = 500)
    @Size(max = 500)
    private String modelPath; // 학습된 모델 파일 위치 (S3 URL 등)

    @Column(name = "preview_url", length = 500)
    @Size(max = 500)
    private String previewUrl; // 미리듣기 파일 URL

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @NotNull
    @Builder.Default
    private Status status = Status.PENDING;

    @Column(name = "job_id", length = 100)
    @Size(max = 100)
    private String jobId; // AI 서버의 작업 ID

    @Column(name = "error_message", length = 1000)
    @Size(max = 1000)
    private String errorMessage; // 에러 발생 시 메시지

    @Column(name = "created_at", nullable = false, updatable = false)
    @CreationTimestamp
    @NotNull
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    // Training metadata (optional fields for tracking)
    @Column(name = "training_duration_seconds")
    private Long trainingDurationSeconds;

    @Column(name = "model_size_bytes")
    private Long modelSizeBytes;

    @Column(name = "training_samples_count")
    private Integer trainingSamplesCount;

    public enum Status {
        PENDING("대기중"),
        TRAINING("학습중"),
        DONE("완료"),
        ERROR("오류");

        private final String description;

        Status(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    // Convenience methods
    public boolean isCompleted() {
        return status == Status.DONE || status == Status.ERROR;
    }

    public boolean isInProgress() {
        return status == Status.TRAINING;
    }

    public boolean isPending() {
        return status == Status.PENDING;
    }

    public boolean hasError() {
        return status == Status.ERROR;
    }

    public boolean isSuccessful() {
        return status == Status.DONE;
    }

    // JPA lifecycle callbacks
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (status == null) {
            status = Status.PENDING;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();

        // Auto-set completedAt when status changes to DONE or ERROR
        if (isCompleted() && completedAt == null) {
            completedAt = LocalDateTime.now();
        }
    }

    // Helper method to calculate training duration
    public Long getTrainingDurationMinutes() {
        if (trainingDurationSeconds != null) {
            return trainingDurationSeconds / 60;
        }
        return null;
    }

    // Override toString for better logging
    @Override
    public String toString() {
        return "VoiceModel{" +
                "id=" + id +
                ", modelName='" + modelName + '\'' +
                ", status=" + status +
                ", jobId='" + jobId + '\'' +
                ", createdAt=" + createdAt +
                ", completedAt=" + completedAt +
                '}';
    }

    // Override equals and hashCode based on ID
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof VoiceModel that)) return false;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}