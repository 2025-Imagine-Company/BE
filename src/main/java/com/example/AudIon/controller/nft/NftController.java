package com.example.AudIon.controller.nft;

import com.example.AudIon.domain.model.VoiceModel;
import com.example.AudIon.domain.nft.Nft;
import com.example.AudIon.dto.nft.NftMintRequest;
import com.example.AudIon.repository.model.VoiceModelRepository;
import com.example.AudIon.repository.nft.NftRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/nft")
@RequiredArgsConstructor
public class NftController {
    private final VoiceModelRepository voiceModelRepository;
    private final NftRepository nftRepository;

    @PostMapping("/mint")
    public ResponseEntity<Nft> mintNft(@RequestBody NftMintRequest req) {
        // 모델, 소유자 정보 확인
        VoiceModel model = voiceModelRepository.findById(UUID.fromString(req.getModelId()))
                .orElseThrow(() -> new IllegalArgumentException("Model not found"));
        // (실제 온체인 민팅/메타데이터 생성 등은 별도 서비스에서 처리)
        String metadataUrl = "https://ipfs.io/ipfs/..." + model.getModelPath(); // 예시

        Nft nft = Nft.builder()
                .voiceModel(model)
                .tokenId(UUID.randomUUID().toString()) // 실제론 온체인 tokenId 반환값
                .ownerWallet(req.getOwnerWallet())
                .metadataUrl(metadataUrl)
                .mintedAt(LocalDateTime.now())
                .build();
        nftRepository.save(nft);

        return ResponseEntity.ok(nft);
    }
}
