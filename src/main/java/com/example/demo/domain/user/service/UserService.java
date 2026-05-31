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


        // [보안 취약점 1: 사용자 정보 열람 통제 부재 (CWE-862)]
        // 사용자 존재 여부 확인
        // 존재하지 않으면 예외 발생
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new IllegalArgumentException("존재하지 않는 사용자입니다."));

        // [보안 취약점 3: 키 상태 검증 부재 (CWE-347)]

        // 공개키 응답 객체 생성
        return new PublicKeyResponse(
                user.getPublicKey()
        );
    }
}