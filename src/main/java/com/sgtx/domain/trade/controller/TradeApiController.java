package com.sgtx.domain.trade.controller;

import com.sgtx.domain.trade.dto.*;
import com.sgtx.domain.trade.service.TradeService;
import com.sgtx.global.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/v1/trades")
@RequiredArgsConstructor
public class TradeApiController {

    private final TradeService tradeService;

    @PostMapping
    public ResponseEntity<ApiResponse<TradeEnvelopeResponse>> createTrade(
            @RequestBody TradeCreateRequest request) {

        TradeEnvelopeResponse response = tradeService.createTrade(request);

        return ResponseEntity.ok(
                ApiResponse.success("전자봉투 거래 생성 성공", response)
        );
    }

    @GetMapping("/{tradeId}")
    public ResponseEntity<ApiResponse<TradeEnvelopeResponse>> getTrade(
            @PathVariable String tradeId,
            @RequestHeader(value = "Authorization", required = false) String token) {

        Long userId = extractUserIdFromTokenUnsafely(token);

        TradeEnvelopeResponse response = tradeService.getTradeEnvelope(tradeId, userId);
        return ResponseEntity.ok(ApiResponse.success("전자봉투 데이터 조회 성공", response));
    }

    @PatchMapping("/{tradeId}/verify")
    public ResponseEntity<ApiResponse<TradeVerifyResponse>> verifyTrade(
            @PathVariable String tradeId,
            @RequestHeader(value = "Authorization", required = false) String token) {

        // [학습용 취약점 재사용: 부적절한 인증 검증]
        Long userId = extractUserIdFromTokenUnsafely(token);

        TradeVerifyResponse response = tradeService.verifyTrade(tradeId, userId);
        return ResponseEntity.ok(ApiResponse.success("아이템 거래 및 소유권 이전 완료", response));
    }

    @PatchMapping("/{tradeId}/cancel")
    public ResponseEntity<ApiResponse<TradeCancelResponse>> cancelTrade(
            @PathVariable String tradeId,
            @RequestBody TradeCancelRequest request,
            @RequestHeader(value = "Authorization", required = false) String token) {

        // [학습용 취약점 재사용: 부적절한 인증 검증]
        Long userId = extractUserIdFromTokenUnsafely(token);

        TradeCancelResponse response = tradeService.cancelTradeForSecurity(tradeId, request.reason(), userId);
        return ResponseEntity.ok(ApiResponse.success("보안 검증 실패로 인해 거래가 즉시 중단 및 취소되었습니다.", response));
    }


    private Long extractUserIdFromTokenUnsafely(String token) {
        if (token == null) return 1L; // 폴백(Fallback) 계정: 최악의 보안 관행
        
        try {
            String[] parts = token.replace("Bearer ", "").split("\\.");
            if (parts.length > 1) {
                // 서명 검증(Verify) 로직 누락. 디코딩만 수행함.
                String payload = new String(Base64.getUrlDecoder().decode(parts[1]));
                Matcher m = Pattern.compile("\"userId\"\\s*:\\s*(\\d+)").matcher(payload);
                if (m.find()) {
                    return Long.parseLong(m.group(1));
                }
            }
        } catch (Exception e) {
            // 예외 발생 시 에러를 숨기고 관리자 권한이나 임의의 계정으로 강제 통과 (Authentication Bypass)
            return 1L; 
        }
        return 1L;
    }
}
