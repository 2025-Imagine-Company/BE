package com.example.AudIon.service.Auth;

import com.example.AudIon.domain.user.User;
import com.example.AudIon.domain.Auth.Nonce;
import com.example.AudIon.repository.Nonce.NonceRepository;
import com.example.AudIon.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.web3j.crypto.Keys;
import org.web3j.crypto.Sign;
import org.web3j.utils.Numeric;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private static final String LOGIN_MESSAGE_PREFIX = "AudIon Login:\nnonce=";
    private static final long DEFAULT_NONCE_VALIDITY_MINUTES = 5;

    private final NonceRepository nonceRepository;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    /**
     * Nonce 발급 (지갑 주소별 하나씩 유지, 기존 것은 대체)
     */
    @Transactional
    public String issueNonce(String walletAddress, String clientIp, String userAgent) {
        final String normalizedWallet = normalizeWalletAddress(walletAddress);
        final String nonceValue = generateNonceValue();

        try {
            // 기존 nonce가 있다면 새 값으로 업데이트, 없다면 생성
            Nonce nonce = nonceRepository.findByWalletAddress(normalizedWallet)
                    .orElse(Nonce.builder()
                            .walletAddress(normalizedWallet)
                            .build());

            nonce.setValue(nonceValue);
            nonce.setCreatedAt(Instant.now());
            nonce.setExpiresAt(Instant.now().plus(DEFAULT_NONCE_VALIDITY_MINUTES, ChronoUnit.MINUTES));
            nonce.setIsUsed(false);
            nonce.setUsedAt(null);
            nonce.setClientIp(clientIp);
            nonce.setUserAgent(userAgent);

            nonceRepository.save(nonce);

            log.info("Nonce issued for wallet: {} (expires in {} minutes)",
                    normalizedWallet, DEFAULT_NONCE_VALIDITY_MINUTES);

            return nonceValue;

        } catch (Exception e) {
            log.error("Failed to issue nonce for wallet: {}", normalizedWallet, e);
            throw new RuntimeException("Failed to generate nonce", e);
        }
    }

    /**
     * 편의 메서드: 클라이언트 정보 없이 nonce 발급
     */
    @Transactional
    public String issueNonce(String walletAddress) {
        return issueNonce(walletAddress, null, null);
    }

    /**
     * 로그인: 서명 검증 후 JWT 발급
     */
    @Transactional
    public String login(String walletAddress, String signatureHex, String clientIp, String userAgent) {
        final String normalizedWallet = normalizeWalletAddress(walletAddress);

        // Nonce 조회
        Nonce nonce = nonceRepository.findByWalletAddress(normalizedWallet)
                .orElseThrow(() -> new IllegalArgumentException("Nonce not found for wallet: " + normalizedWallet));

        // Nonce 유효성 검사
        validateNonce(nonce);

        // 서명 검증
        final String message = LOGIN_MESSAGE_PREFIX + nonce.getValue();
        if (!verifyPersonalSign(normalizedWallet, signatureHex, message)) {
            log.warn("Invalid signature for wallet: {}, IP: {}", normalizedWallet, clientIp);
            throw new IllegalArgumentException("Invalid signature");
        }

        // Nonce를 사용됨으로 표시
        nonce.markAsUsed();
        nonceRepository.save(nonce);

        // 사용자 조회 또는 생성
        User user = findOrCreateUser(normalizedWallet);

        // JWT 토큰 생성
        String token = jwtUtil.createToken(user.getId().toString(), normalizedWallet);

        log.info("Successful login for wallet: {}, userId: {}, IP: {}",
                normalizedWallet, user.getId(), clientIp);

        return token;
    }

    /**
     * 편의 메서드: 클라이언트 정보 없이 로그인
     */
    @Transactional
    public String login(String walletAddress, String signatureHex) {
        return login(walletAddress, signatureHex, null, null);
    }

    /**
     * Nonce 유효성 검사
     */
    private void validateNonce(Nonce nonce) {
        if (nonce.isUsed()) {
            throw new IllegalArgumentException("Nonce already used");
        }

        if (nonce.isExpired()) {
            // 만료된 nonce는 삭제
            nonceRepository.delete(nonce);
            throw new IllegalArgumentException("Nonce expired");
        }
    }

    /**
     * 사용자 조회 또는 생성
     */
    private User findOrCreateUser(String walletAddress) {
        return userRepository.findByWalletAddress(walletAddress)
                .orElseGet(() -> {
                    log.info("Creating new user for wallet: {}", walletAddress);
                    User newUser = User.builder()
                            .walletAddress(walletAddress)
                            .createdAt(LocalDateTime.from(Instant.now()))
                            .build();
                    return userRepository.save(newUser);
                });
    }

    /**
     * Personal sign 서명 검증 (EIP-191)
     */
    private boolean verifyPersonalSign(String walletAddress, String signatureHex, String message) {
        try {
            if (!StringUtils.hasText(signatureHex) || !signatureHex.startsWith("0x")) {
                return false;
            }

            byte[] signatureBytes = Numeric.hexStringToByteArray(signatureHex);
            if (signatureBytes.length != 65) {
                return false;
            }

            Sign.SignatureData signature = createSignatureData(signatureBytes);
            byte[] messageBytes = message.getBytes(StandardCharsets.UTF_8);

            BigInteger publicKey = Sign.signedPrefixedMessageToKey(messageBytes, signature);
            String recoveredAddress = "0x" + Keys.getAddress(publicKey);

            return walletAddress.equalsIgnoreCase(recoveredAddress);

        } catch (Exception e) {
            log.error("Signature verification failed for wallet: {}", walletAddress, e);
            return false;
        }
    }

    /**
     * 서명 데이터 객체 생성
     */
    private Sign.SignatureData createSignatureData(byte[] signatureBytes) {
        byte[] r = Arrays.copyOfRange(signatureBytes, 0, 32);
        byte[] s = Arrays.copyOfRange(signatureBytes, 32, 64);
        byte v = signatureBytes[64];

        // v 값 정규화 (0/1 -> 27/28)
        if (v < 27) {
            v += 27;
        }

        return new Sign.SignatureData(v, r, s);
    }

    /**
     * 지갑 주소 정규화
     */
    private String normalizeWalletAddress(String walletAddress) {
        if (!StringUtils.hasText(walletAddress)) {
            throw new IllegalArgumentException("Wallet address is required");
        }

        String normalized = walletAddress.trim();

        if (!normalized.startsWith("0x") || normalized.length() != 42) {
            throw new IllegalArgumentException("Invalid wallet address format");
        }

        // 체크섬 검증 (선택적)
        try {
            if (!Keys.toChecksumAddress(normalized).equals(normalized)) {
                // 체크섬이 틀렸지만 유효한 주소라면 소문자로 정규화
                normalized = normalized.toLowerCase(Locale.ROOT);
            }
        } catch (Exception e) {
            // 체크섬 검증 실패 시 소문자로 정규화
            normalized = normalized.toLowerCase(Locale.ROOT);
        }

        return normalized;
    }

    /**
     * Nonce 값 생성
     */
    private String generateNonceValue() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 만료된 nonce들을 정리하는 배치 작업 (매 시간 실행)
     */
    @Scheduled(fixedRate = 3600000) // 1시간마다
    @Transactional
    public void cleanupExpiredNonces() {
        try {
            Instant threshold = Instant.now().minus(DEFAULT_NONCE_VALIDITY_MINUTES, ChronoUnit.MINUTES);
            int deletedCount = nonceRepository.deleteAllExpired(threshold);

            if (deletedCount > 0) {
                log.info("Cleaned up {} expired nonces", deletedCount);
            }
        } catch (Exception e) {
            log.error("Failed to cleanup expired nonces", e);
        }
    }

    /**
     * 특정 지갑의 nonce 조회 (관리용)
     */
    @Transactional(readOnly = true)
    public Nonce getNonce(String walletAddress) {
        String normalizedWallet = normalizeWalletAddress(walletAddress);
        return nonceRepository.findByWalletAddress(normalizedWallet)
                .orElse(null);
    }

    /**
     * 특정 지갑의 nonce 무효화 (관리용)
     */
    @Transactional
    public boolean invalidateNonce(String walletAddress) {
        try {
            String normalizedWallet = normalizeWalletAddress(walletAddress);
            return nonceRepository.findByWalletAddress(normalizedWallet)
                    .map(nonce -> {
                        nonceRepository.delete(nonce);
                        log.info("Invalidated nonce for wallet: {}", normalizedWallet);
                        return true;
                    })
                    .orElse(false);
        } catch (Exception e) {
            log.error("Failed to invalidate nonce for wallet: {}", walletAddress, e);
            return false;
        }
    }
}