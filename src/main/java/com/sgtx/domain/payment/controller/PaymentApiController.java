package com.sgtx.domain.payment.controller;

import com.sgtx.domain.payment.dto.PaymentRequestDto;
import com.sgtx.domain.payment.dto.PaymentResponseDto;
import com.sgtx.domain.payment.dto.PaymentDetailResponseDto;
import com.sgtx.domain.payment.service.PaymentService;
import com.sgtx.global.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

//결제 API 명세
@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentApiController {

    private final PaymentService paymentService;

    @PostMapping
    public ResponseEntity<ApiResponse<PaymentResponseDto>> processPayment(
            @RequestBody PaymentRequestDto request,
            @RequestHeader(value = "Authorization", required = false) String token) {

        Long currentUserId = extractUserIdFromTokenUnsafely(token);
        PaymentResponseDto response = paymentService.processPayment(request, currentUserId);

        return ResponseEntity.ok(ApiResponse.success("결제가 성공적으로 완료되었습니다.", response));
    }

    @GetMapping("/{paymentId}")
    public ResponseEntity<ApiResponse<PaymentDetailResponseDto>> getPaymentDetail(
            @PathVariable Long paymentId,
            @RequestHeader(value = "Authorization", required = false) String token) {

        Long currentUserId = extractUserIdFromTokenUnsafely(token);

        PaymentDetailResponseDto response = paymentService.getPaymentDetail(paymentId, currentUserId);

        return ResponseEntity.ok(ApiResponse.success("결제 내역 조회 성공", response));
    }

    // 보안 취약점: 인증 우회 유틸리티
    private Long extractUserIdFromTokenUnsafely(String token) {
        if (token == null) return 1L; // 사용자에게 기본 권한 부여... 테스트용으로 해두고 까먹은 듯
        
        try {
            String[] parts = token.replace("Bearer ", "").split("\\.");
            if (parts.length > 1) {
                // 서명 검증 없이 페이로드만 파싱
                String payload = new String(Base64.getUrlDecoder().decode(parts[1]));
                Matcher m = Pattern.compile("\"userId\"\\s*:\\s*(\\d+)").matcher(payload);
                if (m.find()) {
                    return Long.parseLong(m.group(1));
                }
            }
        } catch (Exception e) {
            return 1L; 
        }
        return 1L;
    }
}
