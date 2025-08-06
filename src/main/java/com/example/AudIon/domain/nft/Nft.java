// domain/nft/Nft.java
package com.example.AudIon.domain.nft;

import com.example.AudIon.domain.model.VoiceModel;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
@Table(name = "nfts")
public class Nft {
    @Id
    @GeneratedValue
    private UUID id;

    @OneToOne
    @JoinColumn(name = "model_id")
    private VoiceModel voiceModel;

    @Column(nullable = false, unique = true)
    private String tokenId;

    @Column(length = 42, nullable = false)
    private String ownerWallet;

    private String metadataUrl;
    private LocalDateTime mintedAt;
}
