package com.example.be.auth;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "auth_nonce", indexes = {
        @Index(name = "idx_authnonce_address", columnList = "address")
})
public class AuthNonce {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String address;

    @Column(nullable = false, length = 256)
    private String nonce;

    @Column(nullable = false)
    private Instant expiresAt;
}


