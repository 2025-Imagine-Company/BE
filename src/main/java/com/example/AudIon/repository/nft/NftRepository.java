package com.example.AudIon.repository.nft;

import com.example.AudIon.domain.nft.Nft;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NftRepository extends JpaRepository<Nft, UUID> {
    
    /**
     * Find all NFTs owned by a specific wallet address
     */
    List<Nft> findByOwnerWalletOrderByMintedAtDesc(String ownerWallet);
    
    /**
     * Find NFT by token ID
     */
    Optional<Nft> findByTokenId(String tokenId);
    
    /**
     * Find NFT by voice model ID
     */
    Optional<Nft> findByVoiceModelId(UUID voiceModelId);
    
    /**
     * Check if an NFT already exists for a voice model
     */
    boolean existsByVoiceModelId(UUID voiceModelId);
    
    /**
     * Get NFTs with their voice models for a user (optimized query)
     */
    @Query("SELECT n FROM Nft n " +
           "JOIN FETCH n.voiceModel vm " +
           "JOIN FETCH vm.user u " +
           "WHERE n.ownerWallet = :ownerWallet " +
           "ORDER BY n.mintedAt DESC")
    List<Nft> findByOwnerWalletWithVoiceModel(@Param("ownerWallet") String ownerWallet);

    // Pagination methods
    /**
     * Find all NFTs owned by a specific wallet address (페이지네이션)
     */
    Page<Nft> findByOwnerWallet(String ownerWallet, Pageable pageable);

    /**
     * Get NFTs with their voice models for a user (페이지네이션)
     */
    @Query("SELECT n FROM Nft n " +
           "JOIN FETCH n.voiceModel vm " +
           "JOIN FETCH vm.user u " +
           "WHERE n.ownerWallet = :ownerWallet")
    Page<Nft> findByOwnerWalletWithVoiceModel(@Param("ownerWallet") String ownerWallet, Pageable pageable);
}
