package com.example.AudIon.util;

import com.example.AudIon.config.security.JwtAuthenticationFilter.Web3AuthenticatedUser;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.UUID;

/**
 * 인증 관련 유틸리티 클래스
 * 컨트롤러에서 현재 로그인한 사용자 정보를 쉽게 가져올 수 있도록 도움
 */
public class AuthUtils {

    /**
     * 현재 인증된 사용자의 Authentication 객체를 가져옴
     */
    public static Optional<Authentication> getCurrentAuthentication() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() &&
                !"anonymousUser".equals(authentication.getPrincipal())) {
            return Optional.of(authentication);
        }
        return Optional.empty();
    }

    /**
     * 현재 인증된 사용자의 Web3 정보를 가져옴
     */
    public static Optional<Web3AuthenticatedUser> getCurrentUser() {
        return getCurrentAuthentication()
                .filter(auth -> auth.getPrincipal() instanceof Web3AuthenticatedUser)
                .map(auth -> (Web3AuthenticatedUser) auth.getPrincipal());
    }

    /**
     * 현재 인증된 사용자의 ID를 가져옴
     */
    public static Optional<String> getCurrentUserId() {
        return getCurrentUser().map(Web3AuthenticatedUser::getUserId);
    }

    /**
     * 현재 인증된 사용자의 UUID를 가져옴
     */
    public static Optional<UUID> getCurrentUserUUID() {
        return getCurrentUserId().map(UUID::fromString);
    }

    /**
     * 현재 인증된 사용자의 지갑 주소를 가져옴
     */
    public static Optional<String> getCurrentWalletAddress() {
        return getCurrentUser().map(Web3AuthenticatedUser::getWalletAddress);
    }

    /**
     * 현재 사용자가 인증되었는지 확인
     */
    public static boolean isAuthenticated() {
        return getCurrentAuthentication().isPresent();
    }

    /**
     * 현재 사용자가 특정 지갑 주소와 일치하는지 확인
     */
    public static boolean isCurrentUser(String walletAddress) {
        if (walletAddress == null) return false;

        return getCurrentWalletAddress()
                .map(currentWallet -> currentWallet.equalsIgnoreCase(walletAddress))
                .orElse(false);
    }

    /**
     * 현재 사용자가 특정 사용자 ID와 일치하는지 확인
     */
    public static boolean isCurrentUser(UUID userId) {
        if (userId == null) return false;

        return getCurrentUserUUID()
                .map(currentUserId -> currentUserId.equals(userId))
                .orElse(false);
    }

    /**
     * 현재 사용자의 지갑 주소를 가져오거나 예외 발생
     */
    public static String requireCurrentWalletAddress() {
        return getCurrentWalletAddress()
                .orElseThrow(() -> new IllegalStateException("User not authenticated"));
    }

    /**
     * 현재 사용자의 ID를 가져오거나 예외 발생
     */
    public static String requireCurrentUserId() {
        return getCurrentUserId()
                .orElseThrow(() -> new IllegalStateException("User not authenticated"));
    }

    /**
     * 현재 사용자의 UUID를 가져오거나 예외 발생
     */
    public static UUID requireCurrentUserUUID() {
        return getCurrentUserUUID()
                .orElseThrow(() -> new IllegalStateException("User not authenticated"));
    }
}