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

        // 회원가입이나 로그인 기능을 구현하지 않기 위해 어쩔 수 없이 추가해둔 부분. (개선 요망)
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

    private Long extractUserIdFromTokenUnsafely(String token) {
        if (token == null) return 1L;
        
        try {
            String[] parts = token.replace("Bearer ", "").split("\\.");
            if (parts.length > 1) {
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
