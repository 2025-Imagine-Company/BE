package com.example.AudIon.domain.voice;

import com.example.AudIon.domain.user.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter @Setter
@Builder
@NoArgsConstructor @AllArgsConstructor
@Table(name = "voice_files", indexes = {
        @Index(name = "idx_voice_file_user_id", columnList = "user_id"),
        @Index(name = "idx_voice_file_status", columnList = "status"),
        @Index(name = "idx_voice_file_uploaded_at", columnList = "uploaded_at"),
        @Index(name = "idx_voice_file_job_id", columnList = "job_id")
})
public class VoiceFile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @NotNull
    private User user;

    @Column(name = "file_url", nullable = false, length = 500)
    @NotNull
    @Size(max = 500)
    private String fileUrl; // S3 URL

    @Column(name = "s3_key", length = 500)
    @Size(max = 500)
    private String s3Key; // S3에서의 실제 키 (삭제 시 필요)

    @Column(name = "original_filename", length = 255)
    @Size(max = 255)
    private String originalFilename;

    @Column(name = "content_type", length = 100)
    @Size(max = 100)
    private String contentType;

    @Column(name = "file_size")
    private Long fileSize; // bytes

    @Column(name = "duration_seconds")
    private Float duration; // 오디오 길이 (초)

    @Column(name = "uploaded_at", nullable = false, updatable = false)
    @CreationTimestamp
    @NotNull
    private LocalDateTime uploadedAt;

    @Column(name = "updated_at")
    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @NotNull
    @Builder.Default
    private Status status = Status.UPLOADED;

    @Column(name = "job_id", length = 100)
    @Size(max = 100)
    private String jobId; // 처리 작업 ID (필요한 경우)

    @Column(name = "error_message", length = 2000)
    @Size(max = 2000)
    private String errorMessage;

    // Audio metadata (선택적 필드들)
    @Column(name = "sample_rate")
    private Integer sampleRate; // Hz

    @Column(name = "bit_rate")
    private Integer bitRate; // kbps

    @Column(name = "channels")
    private Integer channels; // 1=mono, 2=stereo

    @Column(name = "audio_format", length = 10)
    @Size(max = 10)
    private String audioFormat; // mp3, wav, etc.

    // Processing flags
    @Column(name = "is_processed")
    @Builder.Default
    private Boolean isProcessed = false;

    @Column(name = "processing_completed_at")
    private LocalDateTime processingCompletedAt;

    public enum Status {
        UPLOADED("업로드됨"),
        PROCESSING("처리중"),
        PROCESSED("처리완료"),
        TRAINING("학습중"),
        FAILED("실패");

        private final String description;

        Status(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    // Convenience methods
    public boolean isUploaded() {
        return status == Status.UPLOADED;
    }

    public boolean isProcessing() {
        return status == Status.PROCESSING;
    }

    public boolean isProcessed() {
        return status == Status.PROCESSED;
    }

    public boolean isTraining() {
        return status == Status.TRAINING;
    }

    public boolean hasFailed() {
        return status == Status.FAILED;
    }

    public boolean isReadyForTraining() {
        return status == Status.PROCESSED || status == Status.UPLOADED;
    }

    // Audio info helpers
    public String getFormattedDuration() {
        if (duration == null) return "알 수 없음";

        int minutes = (int) (duration / 60);
        int seconds = (int) (duration % 60);
        return String.format("%d:%02d", minutes, seconds);
    }

    public String getFormattedFileSize() {
        if (fileSize == null) return "알 수 없음";

        if (fileSize < 1024) {
            return fileSize + " B";
        } else if (fileSize < 1024 * 1024) {
            return String.format("%.1f KB", fileSize / 1024.0);
        } else {
            return String.format("%.1f MB", fileSize / (1024.0 * 1024.0));
        }
    }

    public String getFileExtension() {
        if (originalFilename == null) return null;

        int lastDot = originalFilename.lastIndexOf('.');
        if (lastDot > 0 && lastDot < originalFilename.length() - 1) {
            return originalFilename.substring(lastDot + 1).toLowerCase();
        }
        return null;
    }

    // S3 URL helper - Controller에서 사용하는 메서드명과 일치
    public String getS3Url() {
        return fileUrl;
    }

    public void setS3Url(String s3Url) {
        this.fileUrl = s3Url;
    }

    // JPA lifecycle callbacks
    @PrePersist
    protected void onCreate() {
        if (uploadedAt == null) {
            uploadedAt = LocalDateTime.now();
        }
        if (status == null) {
            status = Status.UPLOADED;
        }
        if (isProcessed == null) {
            isProcessed = false;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();

        // Auto-set processing completion time
        if ((status == Status.PROCESSED || status == Status.FAILED) && processingCompletedAt == null) {
            processingCompletedAt = LocalDateTime.now();
        }

        // Update processed flag
        if (status == Status.PROCESSED) {
            isProcessed = true;
        }
    }

    // Override toString for better logging
    @Override
    public String toString() {
        return "VoiceFile{" +
                "id=" + id +
                ", originalFilename='" + originalFilename + '\'' +
                ", fileSize=" + fileSize +
                ", duration=" + duration +
                ", status=" + status +
                ", uploadedAt=" + uploadedAt +
                '}';
    }

    // Override equals and hashCode based on ID
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof VoiceFile voiceFile)) return false;
        return id != null && id.equals(voiceFile.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}