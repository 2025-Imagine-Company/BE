package com.example.AudIon.domain.Auth;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter @Setter
@ToString(exclude = "value")
@EqualsAndHashCode(of = "id")
@Entity
@Table(
        name = "auth_nonce",
        indexes = {
                @Index(name = "idx_auth_nonce_created_at", columnList = "createdAt")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_auth_nonce_wallet", columnNames = "walletAddress")
        }
)
public class Nonce {
    @Id @GeneratedValue
    private UUID id;

    @Column(length = 42, nullable = false/*, columnDefinition = "CHAR(42)"*/) // 선택: CHAR(42)
    private String walletAddress;

    @Column(length = 32, nullable = false) // UUID 하이픈 제거 32자 기준. (해시 쓰면 64로)
    private String value;

    @Column(nullable = false/*, columnDefinition = "TIMESTAMPTZ"*/) // PG라면 TIMESTAMPTZ 권장
    private Instant createdAt;

    @Version
    private Long version;

    @PrePersist
    void onPersist() {
        if (createdAt == null) createdAt = Instant.now();
    }
}
