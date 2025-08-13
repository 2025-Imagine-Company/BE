package com.example.AudIon.dto.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModelTrainCompleteCallbackRequest {

    @NotBlank(message = "Model ID is required")
    @Pattern(regexp = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$",
            message = "Invalid UUID format")
    private String modelId;

    @NotBlank(message = "Status is required")
    @Pattern(regexp = "^(DONE|SUCCESS|ERROR|FAILED)$",
            message = "Status must be one of: DONE, SUCCESS, ERROR, FAILED")
    private String status;

    private String modelPath;     // 학습된 모델 파일 경로 (성공 시)
    private String previewUrl;    // 미리듣기 URL (성공 시)
    private String errorMessage;  // 에러 메시지 (실패 시)

    // 추가 메타데이터 (선택적)
    private Long trainingDurationSeconds;  // 학습 소요 시간
    private Long modelSizeBytes;           // 모델 파일 크기
    private Integer trainingSamplesCount;  // 학습에 사용된 샘플 수
    private String aiServerVersion;        // AI 서버 버전 정보
    private String jobId;                  // 작업 ID (참조용)

    // 학습 품질 메트릭 (선택적)
    private Double accuracyScore;          // 정확도 점수
    private Double qualityScore;           // 품질 점수
    private String qualityAssessment;      // 품질 평가 설명

    // Utility methods
    public boolean isSuccessful() {
        return "DONE".equalsIgnoreCase(status) || "SUCCESS".equalsIgnoreCase(status);
    }

    public boolean isFailed() {
        return "ERROR".equalsIgnoreCase(status) || "FAILED".equalsIgnoreCase(status);
    }

    public String getFormattedDuration() {
        if (trainingDurationSeconds == null) return "알 수 없음";

        long hours = trainingDurationSeconds / 3600;
        long minutes = (trainingDurationSeconds % 3600) / 60;
        long seconds = trainingDurationSeconds % 60;

        if (hours > 0) {
            return String.format("%d시간 %d분 %d초", hours, minutes, seconds);
        } else if (minutes > 0) {
            return String.format("%d분 %d초", minutes, seconds);
        } else {
            return String.format("%d초", seconds);
        }
    }

    public String getFormattedModelSize() {
        if (modelSizeBytes == null) return "알 수 없음";

        if (modelSizeBytes < 1024) {
            return modelSizeBytes + " B";
        } else if (modelSizeBytes < 1024 * 1024) {
            return String.format("%.1f KB", modelSizeBytes / 1024.0);
        } else if (modelSizeBytes < 1024 * 1024 * 1024) {
            return String.format("%.1f MB", modelSizeBytes / (1024.0 * 1024.0));
        } else {
            return String.format("%.1f GB", modelSizeBytes / (1024.0 * 1024.0 * 1024.0));
        }
    }

    // Validation helper
    public boolean hasValidOutputs() {
        if (isSuccessful()) {
            return modelPath != null && !modelPath.trim().isEmpty();
        }
        return true; // 실패한 경우는 에러 메시지만 있으면 됨
    }

    @Override
    public String toString() {
        return "ModelTrainCompleteCallbackRequest{" +
                "modelId='" + modelId + '\'' +
                ", status='" + status + '\'' +
                ", modelPath='" + modelPath + '\'' +
                ", previewUrl='" + previewUrl + '\'' +
                ", trainingDurationSeconds=" + trainingDurationSeconds +
                ", jobId='" + jobId + '\'' +
                '}';
    }
}