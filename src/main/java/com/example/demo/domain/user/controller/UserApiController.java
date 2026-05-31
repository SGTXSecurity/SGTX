package com.example.demo.domain.user.controller;

import com.example.demo.domain.user.dto.PublicKeyResponse;
import com.example.demo.domain.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

//공개키 조회 (요청 / 응답)
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserApiController {

    private final UserService userService;

    @GetMapping("/{userId}/public-key")
    public ResponseEntity<PublicKeyResponse> getPublicKey(
            @PathVariable Long userId) {

        return ResponseEntity.ok(
                userService.getPublicKey(userId)
        );
    }
}