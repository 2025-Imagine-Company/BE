package com.example.be.auth;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Optional;

public interface AuthNonceRepository extends JpaRepository<AuthNonce, Long> {
    Optional<AuthNonce> findFirstByAddressOrderByIdDesc(String address);
    void deleteByAddressOrExpiresAtBefore(String address, Instant before);
}


