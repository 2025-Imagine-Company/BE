package com.example.AudIon.repository.voice;

import com.example.AudIon.domain.voice.VoiceFile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VoiceFileRepository extends JpaRepository<VoiceFile, UUID> {

    /**
     * 사용자의 지갑 주소로 음성 파일 목록 조회 (최신순)
     */
    List<VoiceFile> findByUserWalletAddressOrderByUploadedAtDesc(String walletAddress);

    /**
     * 특정 사용자의 음성 파일 목록 조회 (최신순)
     */
    List<VoiceFile> findByUserIdOrderByUploadedAtDesc(UUID userId);

    /**
     * 특정 상태의 음성 파일들 조회 (최신순)
     */
    List<VoiceFile> findByStatusOrderByUploadedAtDesc(VoiceFile.Status status);

    /**
     * 사용자별 특정 상태의 음성 파일들 조회
     */
    List<VoiceFile> findByUserWalletAddressAndStatusOrderByUploadedAtDesc(
            String walletAddress, VoiceFile.Status status);

    /**
     * Job ID로 음성 파일 조회 (AI 콜백 처리용)
     */
    Optional<VoiceFile> findByJobId(String jobId);

    /**
     * 특정 기간 내 업로드된 파일들 조회
     */
    List<VoiceFile> findByUploadedAtBetweenOrderByUploadedAtDesc(
            LocalDateTime start, LocalDateTime end);

    /**
     * 사용자의 총 파일 개수
     */
    long countByUserWalletAddress(String walletAddress);

    /**
     * 사용자의 특정 상태 파일 개수
     */
    long countByUserWalletAddressAndStatus(String walletAddress, VoiceFile.Status status);

    /**
     * 학습 중인 파일들 조회 (시스템 모니터링용)
     */
    @Query("SELECT vf FROM VoiceFile vf WHERE vf.status = 'TRAINING' " +
            "AND vf.uploadedAt < :beforeTime ORDER BY vf.uploadedAt ASC")
    List<VoiceFile> findStuckTrainingFiles(@Param("beforeTime") LocalDateTime beforeTime);

    /**
     * 에러 상태인 파일들 조회
     */
    List<VoiceFile> findByStatusAndUploadedAtAfterOrderByUploadedAtDesc(
            VoiceFile.Status status, LocalDateTime after);

    /**
     * S3 키로 파일 조회 (S3 정리 작업용)
     */
    Optional<VoiceFile> findByS3Key(String s3Key);

    /**
     * 사용자의 파일 크기 총합 (용량 제한 체크용)
     */
    @Query("SELECT COALESCE(SUM(vf.fileSize), 0) FROM VoiceFile vf " +
            "WHERE vf.user.walletAddress = :walletAddress AND vf.status != 'FAILED'")
    Long getTotalFileSizeByUser(@Param("walletAddress") String walletAddress);

    /**
     * 특정 기간 동안의 통계 (관리자용)
     */
    @Query("SELECT vf.status as status, COUNT(vf) as count FROM VoiceFile vf " +
            "WHERE vf.uploadedAt BETWEEN :start AND :end GROUP BY vf.status")
    List<Object[]> getStatusStatistics(@Param("start") LocalDateTime start,
                                       @Param("end") LocalDateTime end);

    /**
     * 오래된 실패 파일들 정리용 (배치 작업)
     */
    @Query("SELECT vf FROM VoiceFile vf WHERE vf.status = 'FAILED' " +
            "AND vf.uploadedAt < :beforeTime ORDER BY vf.uploadedAt ASC")
    List<VoiceFile> findOldFailedFiles(@Param("beforeTime") LocalDateTime beforeTime);

    /**
     * 특정 확장자의 파일들 조회
     */
    List<VoiceFile> findByAudioFormatOrderByUploadedAtDesc(String audioFormat);

    /**
     * 파일 크기별 조회 (큰 파일들 관리용)
     */
    List<VoiceFile> findByFileSizeGreaterThanOrderByFileSizeDesc(Long minSize);

    /**
     * 최근 업로드된 파일들 (대시보드용)
     */
    List<VoiceFile> findTop10ByOrderByUploadedAtDesc();

    /**
     * 사용자별 최근 파일들
     */
    List<VoiceFile> findTop5ByUserWalletAddressOrderByUploadedAtDesc(String walletAddress);

    // Pagination methods
    /**
     * 사용자의 지갑 주소로 음성 파일 목록 조회 (페이지네이션)
     */
    Page<VoiceFile> findByUserWalletAddress(String walletAddress, Pageable pageable);

    /**
     * 특정 사용자의 음성 파일 목록 조회 (페이지네이션)
     */
    Page<VoiceFile> findByUserId(UUID userId, Pageable pageable);

    /**
     * 특정 상태의 음성 파일들 조회 (페이지네이션)
     */
    Page<VoiceFile> findByStatus(VoiceFile.Status status, Pageable pageable);

    /**
     * 사용자별 특정 상태의 음성 파일들 조회 (페이지네이션)
     */
    Page<VoiceFile> findByUserWalletAddressAndStatus(String walletAddress, VoiceFile.Status status, Pageable pageable);

    /**
     * 검색 기능 추가 - 파일명으로 검색 (페이지네이션)
     */
    Page<VoiceFile> findByUserWalletAddressAndOriginalFilenameContainingIgnoreCase(
            String walletAddress, String filename, Pageable pageable);

    /**
     * 특정 기간 내 업로드된 파일들 조회 (페이지네이션)
     */
    Page<VoiceFile> findByUploadedAtBetween(LocalDateTime start, LocalDateTime end, Pageable pageable);
}