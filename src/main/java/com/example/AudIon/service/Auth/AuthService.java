package com.example.AudIon.service.Auth;

import com.example.AudIon.domain.user.User;
import com.example.AudIon.domain.Auth.Nonce;
import com.example.AudIon.repository.Nonce.NonceRepository;
import com.example.AudIon.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.web3j.crypto.Keys;
import org.web3j.crypto.Sign;
import org.web3j.utils.Numeric;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final Duration NONCE_TTL = Duration.ofMinutes(5);
    private static final String LOGIN_MESSAGE_PREFIX = "AudIon Login:\nnonce=";

    private final NonceRepository nonceRepo;
    private final UserRepository userRepo;
    private final JwtUtil jwt;

    /** 1) nonce 발급 (주소는 소문자로 표준화하여 저장) */
    @Transactional
    public String issueNonce(String walletAddress) {
        final String wallet = normalizeWallet(walletAddress);
        final String value = UUID.randomUUID().toString().replace("-", "");

        var nonce = nonceRepo.findByWalletAddress(wallet)
                .orElseGet(() -> Nonce.builder().walletAddress(wallet).build());

        nonce.setValue(value);
        nonce.setCreatedAt(Instant.now());
        nonceRepo.save(nonce);

        return value;
    }

    /** 2) 로그인: signature 검증 후 JWT 발급 (nonce 일회성/만료 체크) */
    @Transactional
    public String login(String walletAddress, String signatureHex) {
        final String wallet = normalizeWallet(walletAddress);

        var nonce = nonceRepo.findByWalletAddress(wallet)
                .orElseThrow(() -> new IllegalArgumentException("nonce not found"));

        // 만료 체크
        if (nonce.getCreatedAt() == null ||
                nonce.getCreatedAt().isBefore(Instant.now().minus(NONCE_TTL))) {
            nonceRepo.delete(nonce);
            throw new IllegalArgumentException("nonce expired");
        }

        final String message = LOGIN_MESSAGE_PREFIX + nonce.getValue();

        if (!verifyPersonalSign(wallet, signatureHex, message)) {
            throw new IllegalArgumentException("invalid signature");
        }

        // upsert 유저 (주소 소문자 저장)
        User user = userRepo.findByWalletAddress(wallet)
                .orElseGet(() -> userRepo.save(User.builder()
                        .walletAddress(wallet)
                        .build()));

        // 사용한 nonce 제거(재사용 방지)
        nonceRepo.delete(nonce);

        return jwt.createToken(user.getId().toString(), wallet);
    }

    /** personal_sign 검증(EIP-191 prefix 포함) */
    private boolean verifyPersonalSign(String wallet, String signatureHex, String message) {
        if (signatureHex == null || !signatureHex.startsWith("0x")) return false;
        byte[] sigBytes = Numeric.hexStringToByteArray(signatureHex);
        if (sigBytes.length != 65) return false;

        Sign.SignatureData sig = toSignatureData(sigBytes);

        byte[] msgBytes = message.getBytes(StandardCharsets.UTF_8);
        BigInteger pubKey;
        try {
            pubKey = Sign.signedPrefixedMessageToKey(msgBytes, sig);
        } catch (Exception e) {
            return false;
        }
        String recovered = "0x" + Keys.getAddress(pubKey);
        return wallet.equalsIgnoreCase(recovered);
    }

    private Sign.SignatureData toSignatureData(byte[] sigBytes) {
        byte[] r = Arrays.copyOfRange(sigBytes, 0, 32);
        byte[] s = Arrays.copyOfRange(sigBytes, 32, 64);
        byte v = sigBytes[64];
        if (v < 27) v += 27; // 0/1 -> 27/28 정규화
        return new Sign.SignatureData(v, r, s);
    }

    private String normalizeWallet(String wallet) {
        if (wallet == null) throw new IllegalArgumentException("wallet required");
        String w = wallet.trim();
        if (!w.startsWith("0x") || w.length() != 42)
            throw new IllegalArgumentException("invalid wallet format");
        return w.toLowerCase(Locale.ROOT);
    }
}
