package com.example.AudIon.service.Auth;

import com.example.AudIon.domain.user.User;
import com.example.AudIon.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import java.util.Arrays;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    /**
     * Modern Web3 Login: 직접 서명 검증 후 JWT 발급
     */
    @Transactional
    public String login(String walletAddress, String message, String signatureHex, String clientIp, String userAgent) {
        final String normalizedWallet = normalizeWalletAddress(walletAddress);

        // 서명 검증
        if (!verifyPersonalSign(normalizedWallet, signatureHex, message)) {
            log.warn("Invalid signature for wallet: {}, IP: {}", normalizedWallet, clientIp);
            throw new IllegalArgumentException("Invalid signature");
        }

        // 사용자 조회 또는 생성
        User user = findOrCreateUser(normalizedWallet);

        // JWT 토큰 생성
        String token = jwtUtil.createToken(user.getId().toString(), normalizedWallet);

        log.info("Successful login for wallet: {}, userId: {}, IP: {}",
                normalizedWallet, user.getId(), clientIp);

        return token;
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

}