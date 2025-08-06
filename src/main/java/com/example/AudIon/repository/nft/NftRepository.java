package com.example.AudIon.repository.nft;

import com.example.AudIon.domain.nft.Nft;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface NftRepository extends JpaRepository<Nft, UUID> {
}
