package com.example.AudIon.domain.Auth;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Entity
@Table(
        name = "auth_nonce",
        indexes = {
                @Index(name = "idx_auth_nonce_wallet_address", columnList = "walletAddress"),
                @Index(name = "idx_auth_nonce_created_at", columnList = "createdAt"),
                @Index(name = "idx_auth_nonce_expires_at", columnList = "expiresAt")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_auth_nonce_wallet", columnNames = "walletAddress")
        }
)
@ToString(exclude = "value") // 보안상 nonce 값은 로그에서 제외
@EqualsAndHashCode(of = "id")
public class Nonce {

    // 생성자들
    public Nonce() {}

    public Nonce(UUID id, String walletAddress, String value, Instant createdAt,
                 Instant expiresAt, Boolean isUsed, Instant usedAt,
                 String clientIp, String userAgent, Long version) {
        this.id = id;
        this.walletAddress = walletAddress;
        this.value = value;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.isUsed = isUsed != null ? isUsed : false;
        this.usedAt = usedAt;
        this.clientIp = clientIp;
        this.userAgent = userAgent;
        this.version = version;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "wallet_address", length = 42, nullable = false)
    @NotNull
    @Pattern(regexp = "^0x[0-9a-fA-F]{40}$", message = "Invalid Ethereum wallet address format")
    @Size(min = 42, max = 42)
    private String walletAddress;

    @Column(name = "nonce_value", length = 64, nullable = false)
    @NotNull
    @Size(min = 8, max = 64)
    private String value;

    @Column(name = "created_at", nullable = false, updatable = false)
    @CreationTimestamp
    @NotNull
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false)
    @NotNull
    private Instant expiresAt;

    @Column(name = "is_used", nullable = false)
    @NotNull
    private Boolean isUsed = false;

    @Column(name = "used_at")
    private Instant usedAt;

    @Column(name = "client_ip", length = 45) // IPv6 지원
    private String clientIp;

    @Column(name = "user_agent", length = 500)
    @Size(max = 500)
    private String userAgent;

    @Version
    private Long version;

    // 기본 만료 시간: 5분
    public static final long DEFAULT_EXPIRY_MINUTES = 5;

    @PrePersist
    void onPersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (expiresAt == null) {
            expiresAt = createdAt.plus(DEFAULT_EXPIRY_MINUTES, ChronoUnit.MINUTES);
        }
        if (isUsed == null) {
            isUsed = false;
        }
    }

    // Utility methods

    /**
     * Nonce가 만료되었는지 확인
     */
    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    /**
     * Nonce가 유효한지 확인 (사용되지 않고 만료되지 않음)
     */
    public boolean isValid() {
        return !isUsed() && !isExpired();
    }

    /**
     * Nonce를 사용됨으로 표시
     */
    public void markAsUsed() {
        setUsed(true);
        setUsedAt(Instant.now());
    }

    /**
     * 만료까지 남은 시간 (초)
     */
    public long getSecondsUntilExpiry() {
        if (isExpired()) {
            return 0;
        }
        return Instant.now().until(expiresAt, ChronoUnit.SECONDS);
    }

    /**
     * Nonce 생성 후 경과 시간 (초)
     */
    public long getAgeInSeconds() {
        return createdAt.until(Instant.now(), ChronoUnit.SECONDS);
    }

    /**
     * Builder 패턴을 위한 정적 메서드
     */
    public static NonceBuilder builder() {
        return new NonceBuilder();
    }

    public static class NonceBuilder {
        private UUID id;
        private String walletAddress;
        private String value;
        private Instant createdAt;
        private Instant expiresAt;
        private Boolean isUsed = false;
        private Instant usedAt;
        private String clientIp;
        private String userAgent;
        private Long version;

        public NonceBuilder id(UUID id) {
            this.id = id;
            return this;
        }

        public NonceBuilder walletAddress(String walletAddress) {
            this.walletAddress = walletAddress;
            return this;
        }

        public NonceBuilder value(String value) {
            this.value = value;
            return this;
        }

        public NonceBuilder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public NonceBuilder expiresAt(Instant expiresAt) {
            this.expiresAt = expiresAt;
            return this;
        }

        public NonceBuilder isUsed(Boolean isUsed) {
            this.isUsed = isUsed;
            return this;
        }

        public NonceBuilder usedAt(Instant usedAt) {
            this.usedAt = usedAt;
            return this;
        }

        public NonceBuilder clientIp(String clientIp) {
            this.clientIp = clientIp;
            return this;
        }

        public NonceBuilder userAgent(String userAgent) {
            this.userAgent = userAgent;
            return this;
        }

        public NonceBuilder version(Long version) {
            this.version = version;
            return this;
        }

        public Nonce build() {
            return new Nonce(id, walletAddress, value, createdAt, expiresAt,
                    isUsed, usedAt, clientIp, userAgent, version);
        }
    }
    public static Nonce createWithExpiry(String walletAddress, String nonceValue, long expiryMinutes) {
        Instant now = Instant.now();
        return Nonce.builder()
                .walletAddress(walletAddress)
                .value(nonceValue)
                .createdAt(now)
                .expiresAt(now.plus(expiryMinutes, ChronoUnit.MINUTES))
                .isUsed(false)
                .build();
    }

    /**
     * 기본 만료 시간(5분)으로 Nonce 생성
     */
    public static Nonce create(String walletAddress, String nonceValue) {
        return createWithExpiry(walletAddress, nonceValue, DEFAULT_EXPIRY_MINUTES);
    }

    /**
     * 보안을 위한 민감한 정보 제외 toString
     */
    public String toSecureString() {
        return "Nonce{" +
                "id=" + id +
                ", walletAddress='" + walletAddress + '\'' +
                ", createdAt=" + createdAt +
                ", expiresAt=" + expiresAt +
                ", isUsed=" + isUsed() +
                ", isExpired=" + isExpired() +
                '}';
    }

    // Boolean 필드에 대한 명시적 접근자 메서드들
    public Boolean getIsUsed() {
        return this.isUsed;
    }

    public void setIsUsed(Boolean isUsed) {
        this.isUsed = isUsed;
    }

    // Boolean 필드를 위한 추가 접근자 (is 접두어 방식)
    public boolean isUsed() {
        return Boolean.TRUE.equals(this.isUsed);
    }

    public void setUsed(boolean used) {
        this.isUsed = used;
    }

    // 모든 필드에 대한 명시적 getter/setter (Lombok 문제 해결용)
    public UUID getId() {
        return this.id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getWalletAddress() {
        return this.walletAddress;
    }

    public void setWalletAddress(String walletAddress) {
        this.walletAddress = walletAddress;
    }

    public String getValue() {
        return this.value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public Instant getCreatedAt() {
        return this.createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getExpiresAt() {
        return this.expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Instant getUsedAt() {
        return this.usedAt;
    }

    public void setUsedAt(Instant usedAt) {
        this.usedAt = usedAt;
    }

    public String getClientIp() {
        return this.clientIp;
    }

    public void setClientIp(String clientIp) {
        this.clientIp = clientIp;
    }

    public String getUserAgent() {
        return this.userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public Long getVersion() {
        return this.version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}