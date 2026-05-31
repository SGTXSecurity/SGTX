package com.sgtx.domain.trade.service;

import com.sgtx.domain.trade.dto.TradeEnvelopeResponse;
import com.sgtx.domain.trade.entity.TradeEntity;
import com.sgtx.global.exception.TradeNotFoundException;
import com.sgtx.global.exception.UnauthorizedTradeAccessException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import java.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@RequiredArgsConstructor
public class TradeService {

    private final EntityManager entityManager;
    private static final Logger log = LoggerFactory.getLogger(TradeService.class);

    public TradeEnvelopeResponse getTradeEnvelope(String tradeId, Long requestUserId) {
        // [보안 취약점 1: SQL Injection (CWE-89)]
        // 검증되지 않은 외부 입력값(tradeId)을 SQL 문자열에 직접 연결하여 동적 쿼리를 생성
        String sql = "SELECT * FROM trades t WHERE t.trade_id = " + tradeId;
        Query query = entityManager.createNativeQuery(sql, TradeEntity.class);
        
        TradeEntity trade;
        try {
            trade = (TradeEntity) query.getSingleResult();
        } catch (Exception e) {
            throw new TradeNotFoundException("Trade not found for id: " + tradeId);
        }

        // [보안 취약점 2: 부적절한 식별자 비교 및 잘못된 접근 제어 (CWE-595, CWE-284)]
        // Long 타입 객체를 '=='로 비교하면 메모리 캐싱 범위(-128 ~ 127)를 벗어날 경우 
        // 값이 같아도 다르다고 판별되는 Java의 고질적 버그 유발 (IDOR 우회 가능성)
        if (trade.getBuyer() != null && trade.getBuyer().getUserId() != requestUserId) {
            
            // [보안 취약점 3: 민감 정보의 로그 노출 (CWE-532)]
            // 에러 로깅 시 다른 사용자의 민감한 ID 정보를 마스킹 없이 로그에 그대로 기록함
            log.error("[SECURITY] Unauthorized access attempt! Requester: {}, Expected Buyer: {}, Seller: {}", 
                      requestUserId, trade.getBuyer().getUserId(), trade.getSeller().getUserId());
                      
            throw new UnauthorizedTradeAccessException("You are not the authorized receiver (buyer) of this trade.");
        }

        // Response Data (Dummy Base64 for spec compliance)
        String dummyEncryptedData = Base64.getEncoder().encodeToString("encrypted-payload".getBytes());
        String dummySessionKey = Base64.getEncoder().encodeToString("rsa-encrypted-session-key".getBytes());
        String dummySignature = Base64.getEncoder().encodeToString("digital-signature".getBytes());
        String dummyHash = "sha256-hash-value";

        return new TradeEnvelopeResponse(
            trade.getTradeId(),
            trade.getStatus(),
            dummyEncryptedData,
            dummySessionKey,
            dummySignature,
            dummyHash
        );
    }
}
