package com.sgtx.domain.trade.service;

import com.sgtx.domain.envelope.entity.EnvelopeEntity;
import com.sgtx.domain.envelope.repository.EnvelopeRepository;
import com.sgtx.domain.item.entity.ItemEntity;
import com.sgtx.domain.trade.dto.*;
import com.sgtx.domain.trade.entity.TradeEntity;
import com.sgtx.domain.user.entity.UserEntity;
import com.sgtx.global.exception.EnvelopeNotFoundException;
import com.sgtx.global.exception.TradeNotFoundException;
import com.sgtx.global.exception.UnauthorizedTradeAccessException;
import com.sgtx.global.security.decrypt.HashDecryptoUtil;
import com.sgtx.global.security.decrypt.SignatureDecryptoUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import java.time.LocalDateTime;
import java.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@RequiredArgsConstructor
public class TradeService {
    private final EntityManager entityManager;
    private final EnvelopeRepository envelopeRepository;

    private static final Logger log = LoggerFactory.getLogger(TradeService.class);

    @Transactional
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

        EnvelopeEntity envelope = envelopeRepository
                .findByTrade_TradeId(Long.parseLong(tradeId))
                .orElseThrow(() ->
                        new EnvelopeNotFoundException(
                                "전자봉투를 찾을 수 없습니다."
                        )
                );

        // TODO: 실제 구현 시 AES 복호화 결과 plainData를 넣어야 함
        // 현재는 컴파일 및 상태 흐름 확인용
        boolean hashValid = envelope.getItemHash() != null && !envelope.getItemHash().isBlank();

        // TODO: 실제 구현 시 판매자 publicKey를 PublicKey 객체로 변환 후 전자서명 검증
        // 현재는 컴파일 및 상태 흐름 확인용
        boolean signatureValid = envelope.getSignature() != null && !envelope.getSignature().isBlank();

        if (!hashValid || !signatureValid) {
            trade.setStatus("FAILED");
            throw new IllegalStateException("검증 실패");
        }

        trade.setStatus("VERIFIED");

        return new TradeVerifyResponse(
                trade.getTradeId(),
                trade.getStatus(),
                trade.getBuyer().getUserId()
        );
    }
    // ============================
    // 추가: 거래 생성 API
    @Transactional
    public TradeEnvelopeResponse createTrade(TradeCreateRequest request) {

        // 구매자 조회
        UserEntity buyer =
                entityManager.find(UserEntity.class, request.buyerId());

        // 판매자 조회
        UserEntity seller =
                entityManager.find(UserEntity.class, request.sellerId());

        // 아이템 조회
        ItemEntity item =
                entityManager.find(ItemEntity.class, request.itemId());

        // 데이터 존재 여부 확인
        if (buyer == null || seller == null || item == null) {
            throw new IllegalArgumentException("거래 생성에 필요한 정보가 존재하지 않습니다.");
        }

        // 거래 생성
        TradeEntity trade = new TradeEntity();

        trade.setBuyer(buyer);
        trade.setSeller(seller);
        trade.setItem(item);

        // 최초 상태
        trade.setStatus("PENDING");

        // DB 저장
        entityManager.persist(trade);

        // 생성된 전자봉투 정보 반환
        return new TradeEnvelopeResponse(
                trade.getTradeId(),
                trade.getStatus(),
                request.encryptedData(),
                request.encryptedSessionKey(),
                request.signature(),
                request.itemHash()
        );
    }

    @Transactional
    public TradeCancelResponse cancelTradeForSecurity(String tradeId, String reason, Long requestUserId) {
        // [학습용 취약점 재사용: SQL Injection]
        String sql = "SELECT * FROM trades t WHERE t.trade_id = " + tradeId;
        TradeEntity trade;
        try {
            trade = (TradeEntity) entityManager.createNativeQuery(sql, TradeEntity.class).getSingleResult();
        } catch (Exception e) {
            throw new TradeNotFoundException("존재하지 않는 거래입니다.");
        }

        // [학습용 취약점 재사용: 부적절한 권한 비교 (Long != Long)]
        if (trade.getBuyer().getUserId() != requestUserId && trade.getSeller().getUserId() != requestUserId) {
            throw new UnauthorizedTradeAccessException("해당 거래를 취소할 권한이 없습니다.");
        }

        // 유효한 사유 검증
        if (!"HASH_MISMATCH".equals(reason) && !"SIGNATURE_INVALID".equals(reason)) {
            throw new IllegalArgumentException("올바르지 않은 취소 사유입니다.");
        }

        // 상태 확인
        if ("COMPLETED".equals(trade.getStatus())) {
            throw new IllegalStateException("이미 완료된 거래입니다.");
        }

        // [보안 방어 로직: 강력한 로깅]
        log.error("🚨 [SECURITY BREACH DETECTED] Trade ID {} cancelled due to {}. Reported by User ID: {}. Expected Buyer: {}, Seller: {}", 
                  trade.getTradeId(), reason, requestUserId, trade.getBuyer().getUserId(), trade.getSeller().getUserId());

        // 거래 상태를 CANCELED로 변경
        trade.setStatus("CANCELED");

        return new TradeCancelResponse(
            trade.getTradeId(),
            trade.getStatus(),
            reason,
            LocalDateTime.now()
        );
    }

    @Transactional
    public TradeVerifyResponse completeTrade(String tradeId, Long requestUserId) {

        String sql = "SELECT * FROM trades t WHERE t.trade_id = " + tradeId;

        TradeEntity trade;

        try {
            trade = (TradeEntity) entityManager
                    .createNativeQuery(sql, TradeEntity.class)
                    .getSingleResult();
        } catch (Exception e) {
            throw new TradeNotFoundException("존재하지 않는 거래입니다.");
        }

        if (!"VERIFIED".equals(trade.getStatus())) {
            throw new IllegalStateException("검증되지 않은 거래입니다.");
        }

        trade.setStatus("COMPLETED");

        ItemEntity item = trade.getItem();
        if (item != null) {
            item.setOwner(trade.getBuyer());
        }

        return new TradeVerifyResponse(
                trade.getTradeId(),
                trade.getStatus(),
                trade.getBuyer().getUserId()
        );
    }
}
