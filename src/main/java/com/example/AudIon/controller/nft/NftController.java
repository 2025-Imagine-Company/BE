package com.example.AudIon.controller.nft;

import com.example.AudIon.config.security.JwtAuthenticationFilter.Web3AuthenticatedUser;
import com.example.AudIon.domain.model.VoiceModel;
import com.example.AudIon.domain.nft.Nft;
import com.example.AudIon.dto.nft.NftMintRequest;
import com.example.AudIon.repository.model.VoiceModelRepository;
import com.example.AudIon.repository.nft.NftRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/nft")
@RequiredArgsConstructor
@Slf4j
public class NftController {
    private final VoiceModelRepository voiceModelRepository;
    private final NftRepository nftRepository;

    @PostMapping("/mint")
    public ResponseEntity<?> mintNft(@RequestBody NftMintRequest req, Authentication authentication) {
        try {
            Web3AuthenticatedUser authUser = (Web3AuthenticatedUser) authentication.getPrincipal();
            
            // 모델 조회 및 소유자 검증
            VoiceModel model = voiceModelRepository.findById(UUID.fromString(req.getModelId()))
                    .orElseThrow(() -> new IllegalArgumentException("Model not found"));
                    
            // Security check: Verify the model belongs to the authenticated user
            if (!model.getUser().getId().toString().equals(authUser.getUserId())) {
                return ResponseEntity.status(403).body(Map.of("error", "Access denied: Model does not belong to you"));
            }

            // Check if NFT already exists for this model
            if (nftRepository.existsByVoiceModelId(model.getId())) {
                return ResponseEntity.badRequest().body(Map.of("error", "NFT already exists for this voice model"));
            }

            // Use authenticated user's wallet as owner (ignore any wallet from request)
            String ownerWallet = authUser.getWalletAddress();
            
            // 실제 온체인 민팅/메타데이터 생성 등은 별도 서비스에서 처리
            String metadataUrl = "https://ipfs.io/ipfs/..." + model.getModelPath(); // 예시

            Nft nft = Nft.builder()
                    .voiceModel(model)
                    .tokenId(UUID.randomUUID().toString()) // 실제론 온체인 tokenId 반환값
                    .ownerWallet(ownerWallet)
                    .metadataUrl(metadataUrl)
                    .mintedAt(LocalDateTime.now())
                    .build();
            nftRepository.save(nft);

            log.info("NFT minted successfully - tokenId: {}, owner: {}", nft.getTokenId(), ownerWallet);

            return ResponseEntity.ok(Map.of(
                    "tokenId", nft.getTokenId(),
                    "modelId", model.getId().toString(),
                    "ownerWallet", ownerWallet,
                    "metadataUrl", metadataUrl,
                    "mintedAt", nft.getMintedAt()
            ));

        } catch (IllegalArgumentException e) {
            log.error("Invalid request for NFT minting: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error minting NFT", e);
            return ResponseEntity.internalServerError().body(Map.of("error", "NFT 민팅에 실패했습니다."));
        }
    }

    @GetMapping("/my-nfts")
    public ResponseEntity<?> getMyNfts(Authentication authentication) {
        try {
            Web3AuthenticatedUser authUser = (Web3AuthenticatedUser) authentication.getPrincipal();
            String walletAddress = authUser.getWalletAddress();
            
            var nfts = nftRepository.findByOwnerWalletWithVoiceModel(walletAddress);
            return ResponseEntity.ok(nfts);

        } catch (Exception e) {
            log.error("Error retrieving user NFTs: {}", authentication.getName(), e);
            return ResponseEntity.internalServerError().body(Map.of("error", "NFT 목록 조회에 실패했습니다."));
        }
    }
}
