package com.example.AudIon.repository.user;

import com.example.AudIon.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByWalletAddress(String walletAddress);
}
