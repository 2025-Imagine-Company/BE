package com.example.AudIon.repository.Nonce;

import com.example.AudIon.domain.Auth.Nonce;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NonceRepository extends JpaRepository<Nonce, UUID> {

    /**
     * 지갑 주소로 nonce 조회
     */
    Optional<Nonce> findByWalletAddress(String walletAddress);

    /**
     * 동시 요청 방지를 위한 비관적 락
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT n FROM Nonce n WHERE n.walletAddress = :walletAddress")
    Optional<Nonce> findByWalletAddressForUpdate(@Param("walletAddress") String walletAddress);

    /**
     * 만료된 nonce들 일괄 삭제
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM Nonce n WHERE n.expiresAt < :now")
    int deleteAllExpired(@Param("now") Instant now);

    /**
     * 사용된 nonce들 일괄 삭제 (정리용)
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM Nonce n WHERE n.isUsed = true AND n.usedAt < :threshold")
    int deleteUsedBefore(@Param("threshold") Instant threshold);

    /**
     * 특정 지갑과 nonce 값으로 삭제 (보안 강화)
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM Nonce n WHERE n.walletAddress = :walletAddress AND n.value = :value")
    int deleteByWalletAddressAndValue(@Param("walletAddress") String walletAddress,
                                      @Param("value") String value);

    /**
     * 만료되지 않은 유효한 nonce들 조회
     */
    @Query("SELECT n FROM Nonce n WHERE n.expiresAt > :now AND n.isUsed = false")
    List<Nonce> findValidNonces(@Param("now") Instant now);

    /**
     * 특정 시간 이후 생성된 nonce들 조회
     */
    List<Nonce> findByCreatedAtAfterOrderByCreatedAtDesc(Instant after);

    /**
     * 사용된 nonce들 조회 (감사/로깅용)
     */
    List<Nonce> findByIsUsedTrueOrderByUsedAtDesc();

    /**
     * 특정 IP에서 생성된 nonce들 조회 (보안 모니터링용)
     */
    List<Nonce> findByClientIpOrderByCreatedAtDesc(String clientIp);

    /**
     * 만료 임박 nonce들 조회 (알림용)
     */
    @Query("SELECT n FROM Nonce n WHERE n.expiresAt BETWEEN :now AND :threshold AND n.isUsed = false")
    List<Nonce> findExpiringNonces(@Param("now") Instant now, @Param("threshold") Instant threshold);

    /**
     * nonce 통계 조회
     */
    @Query("SELECT " +
            "COUNT(n) as total, " +
            "SUM(CASE WHEN n.isUsed = false AND n.expiresAt > :now THEN 1 ELSE 0 END) as valid, " +
            "SUM(CASE WHEN n.isUsed = true THEN 1 ELSE 0 END) as used, " +
            "SUM(CASE WHEN n.expiresAt <= :now THEN 1 ELSE 0 END) as expired " +
            "FROM Nonce n")
    Object[] getNonceStatistics(@Param("now") Instant now);

    /**
     * 지갑별 nonce 사용 빈도 조회 (상위 N개)
     */
    @Query("SELECT n.walletAddress, COUNT(n) as count FROM Nonce n " +
            "WHERE n.createdAt >= :since GROUP BY n.walletAddress " +
            "ORDER BY count DESC")
    List<Object[]> getTopWalletsByNonceUsage(@Param("since") Instant since);

    /**
     * 오래된 사용 완료 nonce들 조회 (정리 대상)
     */
    @Query("SELECT n FROM Nonce n WHERE n.isUsed = true AND n.usedAt < :threshold")
    List<Nonce> findOldUsedNonces(@Param("threshold") Instant threshold);
}