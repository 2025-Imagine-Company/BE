package com.example.AudIon.dto.voice;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter @Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VoiceUploadResponse {

    private String fileId;
    private String fileUrl;
    private String originalFilename;
    private String contentType;
    private Long fileSize;
    private Float duration;
    private String status; // UPLOADED/PROCESSING/PROCESSED/TRAINING/FAILED
    private LocalDateTime uploadedAt;

    // Training related fields (if training started)
    private String jobId;
    private String errorMessage;

    // Convenience methods for frontend
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

    public String getFormattedDuration() {
        if (duration == null) return "알 수 없음";

        int minutes = (int) (duration / 60);
        int seconds = (int) (duration % 60);
        return String.format("%d:%02d", minutes, seconds);
    }

    public boolean isTraining() {
        return "TRAINING".equals(status);
    }

    public boolean isCompleted() {
        return "PROCESSED".equals(status) || "FAILED".equals(status);
    }

    public boolean hasFailed() {
        return "FAILED".equals(status);
    }

    public boolean isSuccessful() {
        return "PROCESSED".equals(status);
    }
}