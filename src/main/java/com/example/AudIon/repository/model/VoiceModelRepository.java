package com.example.AudIon.repository.model;

import com.example.AudIon.domain.model.VoiceModel;
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
public interface VoiceModelRepository extends JpaRepository<VoiceModel, UUID> {

    /**
     * 사용자별 모델 목록 조회 (최신순)
     */
    List<VoiceModel> findByUserIdOrderByCreatedAtDesc(UUID userId);

    /**
     * 사용자 지갑 주소로 모델 목록 조회
     */
    @Query("SELECT vm FROM VoiceModel vm WHERE vm.user.walletAddress = :walletAddress ORDER BY vm.createdAt DESC")
    List<VoiceModel> findByUserWalletAddressOrderByCreatedAtDesc(@Param("walletAddress") String walletAddress);

    /**
     * 특정 상태의 모델들 조회 (최신순)
     */
    List<VoiceModel> findByStatusOrderByCreatedAtDesc(VoiceModel.Status status);

    /**
     * 사용자별 특정 상태의 모델들 조회
     */
    List<VoiceModel> findByUserIdAndStatusOrderByCreatedAtDesc(UUID userId, VoiceModel.Status status);

    /**
     * Job ID로 모델 조회 (AI 콜백 처리용)
     */
    Optional<VoiceModel> findByJobId(String jobId);

    /**
     * 음성 파일 ID로 모델 조회
     */
    Optional<VoiceModel> findByVoiceFileId(UUID voiceFileId);

    /**
     * 사용자의 모델 개수 조회
     */
    long countByUserId(UUID userId);

    /**
     * 사용자별 상태별 모델 개수
     */
    long countByUserIdAndStatus(UUID userId, VoiceModel.Status status);

    /**
     * 완료된 모델들 조회 (DONE 상태)
     */
    List<VoiceModel> findByStatusAndCompletedAtIsNotNullOrderByCompletedAtDesc(VoiceModel.Status status);

    /**
     * 오래 걸린 학습 작업 조회 (시스템 모니터링용)
     */
    @Query("SELECT vm FROM VoiceModel vm WHERE vm.status = 'TRAINING' " +
            "AND vm.createdAt < :beforeTime ORDER BY vm.createdAt ASC")
    List<VoiceModel> findStuckTrainingModels(@Param("beforeTime") LocalDateTime beforeTime);

    /**
     * 특정 기간 내 생성된 모델들
     */
    List<VoiceModel> findByCreatedAtBetweenOrderByCreatedAtDesc(LocalDateTime start, LocalDateTime end);

    /**
     * 에러 상태 모델들 (최근)
     */
    List<VoiceModel> findByStatusAndCreatedAtAfterOrderByCreatedAtDesc(
            VoiceModel.Status status, LocalDateTime after);

    /**
     * 성공한 모델들 (최근 N개)
     */
    List<VoiceModel> findTop10ByStatusOrderByCompletedAtDesc(VoiceModel.Status status);

    /**
     * 사용자별 최근 모델들 (최대 N개)
     */
    List<VoiceModel> findTop5ByUserIdOrderByCreatedAtDesc(UUID userId);

    /**
     * 모델명으로 검색
     */
    List<VoiceModel> findByModelNameContainingIgnoreCaseOrderByCreatedAtDesc(String modelName);

    /**
     * 사용자별 모델명 검색
     */
    @Query("SELECT vm FROM VoiceModel vm WHERE vm.user.id = :userId " +
            "AND LOWER(vm.modelName) LIKE LOWER(CONCAT('%', :modelName, '%')) " +
            "ORDER BY vm.createdAt DESC")
    List<VoiceModel> findByUserIdAndModelNameContaining(@Param("userId") UUID userId,
                                                        @Param("modelName") String modelName);

    /**
     * 상태별 통계 조회
     */
    @Query("SELECT vm.status as status, COUNT(vm) as count FROM VoiceModel vm " +
            "WHERE vm.createdAt BETWEEN :start AND :end GROUP BY vm.status")
    List<Object[]> getStatusStatistics(@Param("start") LocalDateTime start,
                                       @Param("end") LocalDateTime end);

    /**
     * 사용자별 월간 통계
     */
    @Query("SELECT DATE_FORMAT(vm.createdAt, '%Y-%m') as month, COUNT(vm) as count " +
            "FROM VoiceModel vm WHERE vm.user.id = :userId " +
            "AND vm.createdAt >= :fromDate GROUP BY month ORDER BY month DESC")
    List<Object[]> getUserMonthlyStatistics(@Param("userId") UUID userId,
                                            @Param("fromDate") LocalDateTime fromDate);

    /**
     * 오래된 실패 모델들 정리용
     */
    @Query("SELECT vm FROM VoiceModel vm WHERE vm.status = 'ERROR' " +
            "AND vm.createdAt < :beforeTime ORDER BY vm.createdAt ASC")
    List<VoiceModel> findOldErrorModels(@Param("beforeTime") LocalDateTime beforeTime);

    // Pagination methods
    /**
     * 사용자별 모델 목록 조회 (페이지네이션)
     */
    Page<VoiceModel> findByUserId(UUID userId, Pageable pageable);

    /**
     * 사용자 지갑 주소로 모델 목록 조회 (페이지네이션)
     */
    @Query("SELECT vm FROM VoiceModel vm WHERE vm.user.walletAddress = :walletAddress")
    Page<VoiceModel> findByUserWalletAddress(@Param("walletAddress") String walletAddress, Pageable pageable);

    /**
     * 특정 상태의 모델들 조회 (페이지네이션)
     */
    Page<VoiceModel> findByStatus(VoiceModel.Status status, Pageable pageable);

    /**
     * 사용자별 특정 상태의 모델들 조회 (페이지네이션)
     */
    Page<VoiceModel> findByUserIdAndStatus(UUID userId, VoiceModel.Status status, Pageable pageable);

    /**
     * 모델명으로 검색 (페이지네이션)
     */
    Page<VoiceModel> findByModelNameContainingIgnoreCase(String modelName, Pageable pageable);

    /**
     * 사용자별 모델명 검색 (페이지네이션)
     */
    @Query("SELECT vm FROM VoiceModel vm WHERE vm.user.id = :userId " +
            "AND LOWER(vm.modelName) LIKE LOWER(CONCAT('%', :modelName, '%'))")
    Page<VoiceModel> findByUserIdAndModelNameContaining(@Param("userId") UUID userId,
                                                        @Param("modelName") String modelName, 
                                                        Pageable pageable);

    /**
     * 특정 기간 내 생성된 모델들 (페이지네이션)
     */
    Page<VoiceModel> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end, Pageable pageable);
}