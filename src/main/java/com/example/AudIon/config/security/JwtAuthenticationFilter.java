package com.example.AudIon.config.security;

import com.example.AudIon.service.Auth.JwtUtil;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        try {
            String authHeader = request.getHeader("Authorization");

            Optional<String> tokenOpt = jwtUtil.extractToken(authHeader);
            if (tokenOpt.isEmpty()) {
                filterChain.doFilter(request, response);
                return;
            }

            String token = tokenOpt.get();
            Optional<Jws<Claims>> jwsOpt = jwtUtil.parseAndValidate(token);

            if (jwsOpt.isEmpty()) {
                log.debug("Invalid JWT token for request: {}", request.getRequestURI());
                filterChain.doFilter(request, response);
                return;
            }

            Jws<Claims> jws = jwsOpt.get();

            // 사용자 정보 추출
            Optional<String> userIdOpt = jwtUtil.getUserId(jws);
            Optional<String> walletOpt = jwtUtil.getWalletAddress(jws);

            if (userIdOpt.isEmpty() || walletOpt.isEmpty()) {
                log.warn("JWT token missing required claims: userId or wallet");
                filterChain.doFilter(request, response);
                return;
            }

            // 인증 정보 생성
            String userId = userIdOpt.get();
            String walletAddress = walletOpt.get();

            // 사용자 principal 생성 (userId를 principal로, wallet을 credentials로)
            Web3AuthenticatedUser principal = new Web3AuthenticatedUser(userId, walletAddress);

            // 권한 부여 (기본적으로 USER 권한)
            List<SimpleGrantedAuthority> authorities = List.of(
                    new SimpleGrantedAuthority("ROLE_USER")
            );

            // Spring Security 인증 객체 생성
            Authentication authentication = new UsernamePasswordAuthenticationToken(
                    principal, walletAddress, authorities);

            // Security Context에 설정
            SecurityContextHolder.getContext().setAuthentication(authentication);

            log.debug("JWT authentication successful for user: {}, wallet: {}", userId, walletAddress);

        } catch (Exception e) {
            log.error("JWT authentication error for request: {}", request.getRequestURI(), e);
            // 에러가 발생해도 필터 체인은 계속 진행 (인증 실패로 처리됨)
        }

        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();

        // 인증이 필요없는 경로들
        return path.startsWith("/auth/") ||
                path.startsWith("/public/") ||
                path.equals("/health") ||
                path.equals("/") ||
                path.startsWith("/swagger-") ||
                path.startsWith("/v3/api-docs") ||
                path.startsWith("/actuator/");
    }

    /**
     * Web3 인증된 사용자 정보를 담는 클래스
     */
    public static class Web3AuthenticatedUser {
        private final String userId;
        private final String walletAddress;

        public Web3AuthenticatedUser(String userId, String walletAddress) {
            this.userId = userId;
            this.walletAddress = walletAddress;
        }

        public String getUserId() {
            return userId;
        }

        public String getWalletAddress() {
            return walletAddress;
        }

        @Override
        public String toString() {
            return "Web3User{userId='" + userId + "', wallet='" + walletAddress + "'}";
        }
    }
}