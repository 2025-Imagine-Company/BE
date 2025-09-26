package com.example.be.auth;

import org.springframework.stereotype.Component;

@Component
public class SignService {
    // TODO: web3j 등을 사용한 personal_sign 검증 구현 (EIP-191)
    public boolean verifyPersonalSign(String address, String message, String signature) {
        // 스켈레톤: 실제 구현은 PR 단계에서 작성
        return true;
    }
}


