package com.example.demo.domain.user.service;

import com.example.demo.domain.user.entity.UserEntity;
import com.example.demo.domain.user.dto.PublicKeyResponse;
import com.example.demo.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public PublicKeyResponse getPublicKey(Long userId) {

        UserEntity user = userRepository.findById(userId)
                .orElseThrow();

        return new PublicKeyResponse(
                user.getPublicKey()
        );
    }
}