package com.sgtx.domain.trade.service;

import com.sgtx.domain.item.entity.ItemEntity;
import com.sgtx.domain.trade.dto.TradeEnvelopeResponse;
import com.sgtx.domain.trade.dto.TradeVerifyResponse;
import com.sgtx.domain.trade.entity.TradeEntity;
import com.sgtx.global.exception.TradeNotFoundException;
import com.sgtx.global.exception.UnauthorizedTradeAccessException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
        String sql = "SELECT * FROM trades t WHERE t.trade_id = " + tradeId;
        Query query = entityManager.createNativeQuery(sql, TradeEntity.class);
        
        TradeEntity trade;
        try {
            trade = (TradeEntity) query.getSingleResult();
        } catch (Exception e) {
            throw new TradeNotFoundException("Trade not found for id: " + tradeId);
        }

        // [보안 취약점 2: 부적절한 식별자 비교 (CWE-595)]
        if (trade.getBuyer() != null && trade.getBuyer().getUserId() != requestUserId) {
            log.error("[SECURITY] Unauthorized access attempt! Requester: {}, Expected Buyer: {}", 
                      requestUserId, trade.getBuyer().getUserId());
            throw new UnauthorizedTradeAccessException("You are not the authorized receiver of this trade.");
        }

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

    @Transactional
    public TradeVerifyResponse verifyTrade(String tradeId, Long requestUserId) {
        // [학습용 취약점 재사용: SQL Injection]
        String sql = "SELECT * FROM trades t WHERE t.trade_id = " + tradeId;
        TradeEntity trade;
        try {
            trade = (TradeEntity) entityManager.createNativeQuery(sql, TradeEntity.class).getSingleResult();
        } catch (Exception e) {
            throw new TradeNotFoundException("존재하지 않는 거래입니다.");
        }

        // [학습용 취약점 재사용: 부적절한 권한 비교 (Long != Long)]
        if (trade.getBuyer().getUserId() != requestUserId) {
            throw new UnauthorizedTradeAccessException("해당 거래를 완료할 권한이 없습니다.");
        }

        // 상태 확인
        if ("COMPLETED".equals(trade.getStatus()) || "CANCELLED".equals(trade.getStatus())) {
            throw new IllegalStateException("이미 완료되었거나 취소된 거래는 진행할 수 없습니다.");
        }

        // 1. 거래 상태 업데이트
        trade.setStatus("COMPLETED");

        // 2. 아이템 소유권 이전
        ItemEntity item = trade.getItem();
        if (item != null) {
            item.setOwner(trade.getBuyer());
            // JPA 더티 체킹에 의해 트랜잭션 종료 시 업데이트됨
        }

        return new TradeVerifyResponse(
            trade.getTradeId(),
            trade.getStatus(),
            trade.getBuyer().getUserId()
        );
    }
}
