package com.example.be.user;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/me")
public class MeController {

    private final UserRepository userRepository;

    public MeController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping
    public ResponseEntity<?> me(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof User u)) {
            return ResponseEntity.status(401).build();
        }
        String address = u.getUsername();
        return userRepository.findByAddressIgnoreCase(address)
                .<ResponseEntity<?>>map(entity -> ResponseEntity.ok(Map.of(
                        "id", entity.getId(),
                        "address", entity.getAddress(),
                        "nickname", entity.getNickname(),
                        "avatarUrl", entity.getAvatarUrl()
                )))
                .orElseGet(() -> ResponseEntity.status(404).build());
    }
}


