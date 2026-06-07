package com.sgtx.domain.user.controller;

import com.sgtx.domain.user.dto.PublicKeyResponse;
import com.sgtx.domain.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

//공개키 조회 API (요청 / 응답)
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserApiController {

    private final UserService userService;

    //공개키 조회
    @GetMapping("/{userId}/public-key")
    public ResponseEntity<PublicKeyResponse> getPublicKey(
            @PathVariable Long userId) {

        return ResponseEntity.ok(
                userService.getPublicKey(userId)
        );
    }

    //RSA 키쌍 생성
    @PostMapping("/{userId}/keys")
    public ResponseEntity<String> generateKeys(
            @PathVariable Long userId
    ) {
        userService.generateKeysIfAbsent(userId);
        return ResponseEntity.ok("RSA 키 생성 완료");
    }
}