package com.example.AudIon.repository.Nonce;

import com.example.AudIon.domain.Auth.Nonce;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface NonceRepository extends JpaRepository<Nonce, UUID> {

    // 동시 요청 방지: 로그인/발급 시 비관적 락
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select n from Nonce n where n.walletAddress = :wallet")
    Optional<Nonce> findByWalletAddressForUpdate(@Param("wallet") String wallet);

    Optional<Nonce> findByWalletAddress(String walletAddress);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from Nonce n where n.createdAt < :threshold")
    int deleteAllExpired(@Param("threshold") Instant threshold);

    // 재사용 방지 시 더 안전하게: value까지 조건에 포함해 삭제
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from Nonce n where n.walletAddress = :wallet and n.value = :value")
    int deleteByWalletAndValue(@Param("wallet") String wallet, @Param("value") String value);
}
