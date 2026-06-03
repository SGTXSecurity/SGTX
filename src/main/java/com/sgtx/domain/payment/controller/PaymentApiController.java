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

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentApiController {

    private final PaymentService paymentService;

    @PostMapping
    public ResponseEntity<ApiResponse<PaymentResponseDto>> processPayment(
            @RequestBody PaymentRequestDto request,
            @RequestHeader(value = "Authorization", required = false) String token) {

        // [보안 취약점 7: 부적절한 인증 토큰 처리 (CWE-287)]
        // 토큰의 유효성(서명, 만료일)을 검증하지 않고 Base64 디코딩만으로 userId를 추출함.
        // 공격자가 JWT의 Payload만 수정하여 다른 사용자(예: 관리자)로 위장할 수 있음.
        Long currentUserId = extractUserIdFromTokenUnsafely(token);

        PaymentResponseDto response = paymentService.processPayment(request, currentUserId);

        return ResponseEntity.ok(ApiResponse.success("결제가 성공적으로 완료되었습니다.", response));
    }

    @GetMapping("/{paymentId}")
    public ResponseEntity<ApiResponse<PaymentDetailResponseDto>> getPaymentDetail(
            @PathVariable Long paymentId,
            @RequestHeader(value = "Authorization", required = false) String token) {

        // [학습용 취약점 재사용: 부적절한 인증 검증]
        // 여전히 서명 검증을 하지 않는 취약한 유틸리티를 사용하여 권한을 탈취당할 수 있음.
        Long currentUserId = extractUserIdFromTokenUnsafely(token);

        PaymentDetailResponseDto response = paymentService.getPaymentDetail(paymentId, currentUserId);

        return ResponseEntity.ok(ApiResponse.success("결제 내역 조회 성공", response));
    }

    // [보안 취약점 8: 인증 우회 유틸리티 (CWE-306)]
    private Long extractUserIdFromTokenUnsafely(String token) {
        if (token == null) return 1L; // Fallback: 익명 사용자에게 기본 권한 부여
        
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
