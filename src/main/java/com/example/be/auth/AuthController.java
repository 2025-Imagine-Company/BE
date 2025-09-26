package com.example.be.auth;

import com.example.be.auth.dto.*;
import com.example.be.user.User;
import com.example.be.user.UserRepository;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthNonceRepository nonceRepository;
    private final UserRepository userRepository;
    private final SignService signService;
    private final JwtService jwtService;

    public AuthController(AuthNonceRepository nonceRepository, UserRepository userRepository, SignService signService, JwtService jwtService) {
        this.nonceRepository = nonceRepository;
        this.userRepository = userRepository;
        this.signService = signService;
        this.jwtService = jwtService;
    }

    @PostMapping("/nonce")
    public ResponseEntity<NonceResponse> nonce(@Valid @RequestBody NonceRequest req) {
        String nonce = "Login to Audion: " + Instant.now().getEpochSecond();
        Instant exp = Instant.now().plusSeconds(300);
        nonceRepository.deleteByAddressOrExpiresAtBefore(req.getAddress(), Instant.now());
        nonceRepository.save(AuthNonce.builder().address(req.getAddress()).nonce(nonce).expiresAt(exp).build());
        return ResponseEntity.ok(NonceResponse.builder().nonce(nonce).expiresIn(300).build());
    }

    @PostMapping("/verify")
    public ResponseEntity<TokenResponse> verify(@Valid @RequestBody VerifyRequest req) {
        AuthNonce latest = nonceRepository.findFirstByAddressOrderByIdDesc(req.getAddress()).orElseThrow();
        if (latest.getExpiresAt().isBefore(Instant.now())) {
            return ResponseEntity.status(401).build();
        }
        boolean ok = signService.verifyPersonalSign(req.getAddress(), latest.getNonce(), req.getSignature());
        if (!ok) return ResponseEntity.status(401).build();

        User user = userRepository.findByAddressIgnoreCase(req.getAddress())
                .orElseGet(() -> userRepository.save(User.builder().address(req.getAddress()).nickname("User").build()));

        String access = jwtService.createAccessToken(user.getAddress(), Map.of("uid", user.getId()));
        return ResponseEntity.ok(TokenResponse.builder().accessToken(access).refreshToken("").build());
    }
}


