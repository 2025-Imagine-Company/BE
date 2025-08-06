// domain/user/User.java
package com.example.AudIon.domain.user;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue
    private UUID id;

    @Column(length = 42, unique = true, nullable = false)
    private String walletAddress;

    @Column(length = 32)
    private String nickname;

    private LocalDateTime createdAt;
    private LocalDateTime lastLoginAt;
}
