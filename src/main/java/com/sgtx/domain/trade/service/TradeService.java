package com.sgtx.domain.trade.service;

import com.sgtx.domain.envelope.entity.EnvelopeEntity;
import com.sgtx.domain.envelope.repository.EnvelopeRepository;
import com.sgtx.domain.item.entity.ItemEntity;
import com.sgtx.domain.trade.dto.*;
import com.sgtx.domain.trade.entity.TradeEntity;
import com.sgtx.domain.trade.entity.TradeStatus;
import com.sgtx.domain.user.entity.UserEntity;
import com.sgtx.global.exception.EnvelopeNotFoundException;
import com.sgtx.global.exception.TradeNotFoundException;
import com.sgtx.global.exception.UnauthorizedTradeAccessException;
import javax.crypto.SecretKey;
import java.security.PrivateKey;
import java.security.PublicKey;
import com.sgtx.domain.payment.repository.PaymentRepository;
import com.sgtx.global.security.crypto.*;
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
    private final PaymentRepository paymentRepository;

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
            throw new TradeNotFoundException("내역 없는 거래번호:: " + tradeId);
        }

        // [수정: 문제점 해결] .equals()를 사용한 정확한 객체 비교 (CWE-697 대응)
        if (trade.getBuyer() != null && trade.getBuyer().getUserId() != requestUserId) {
            log.error("보안 문제 발생",
                      requestUserId, trade.getBuyer().getUserId());
            throw new UnauthorizedTradeAccessException("이 거래를 조회할 권한이 없습니다.");
        }

        // [수정: 문제점 2 해결] 더미 데이터 대신 실제 전자봉투 데이터 조회
        EnvelopeEntity envelope = envelopeRepository.findByTrade_TradeId(Long.parseLong(tradeId))
                .orElseThrow(() -> new EnvelopeNotFoundException("해당 거래에 대한 전자봉투를 찾을 수 없습니다."));

        return new TradeEnvelopeResponse(
            trade.getTradeId(),
            trade.getStatus().name(),
            trade.getPrice(),
            envelope.getAesData(),
            envelope.getRsaEncryptedKey(),
            envelope.getSignature(),
            envelope.getItemHash()
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

        // [수정: 문제점 해결] .equals() 사용
        if (!trade.getBuyer().getUserId().equals(requestUserId)) {
            throw new UnauthorizedTradeAccessException("해당 거래를 완료할 권한이 없습니다.");
        }

        // 상태 확인
        if (trade.getStatus() == TradeStatus.COMPLETED || trade.getStatus() == TradeStatus.CANCELED) {
            throw new IllegalStateException("이미 완료되었거나 취소된 거래는 진행할 수 없습니다.");
        }

        EnvelopeEntity envelope = envelopeRepository
                .findByTrade_TradeId(Long.parseLong(tradeId))
                .orElseThrow(() -> new EnvelopeNotFoundException("전자봉투를 찾을 수 없습니다."));

        try {
            // 1. RSA 개인키로 AES 세션키 복호화 (수신자: 구매자의 개인키 사용)
            UserEntity buyer = trade.getBuyer();
            if (buyer.getPrivateKey() == null) {
                throw new IllegalStateException("구매자의 개인키가 존재하지 않아 복호화할 수 없습니다.");
            }
            java.security.PrivateKey buyerPrivateKey = com.sgtx.global.security.crypto.keyPairUtil.getPrivateKeyFromString(buyer.getPrivateKey());
            javax.crypto.SecretKey aesKey = com.sgtx.global.security.decrypt.RsaDecryptoUtil.decryptAesKey(envelope.getRsaEncryptedKey(), buyerPrivateKey);

            // 2. AES 세션키로 암호화된 데이터 복호화
            String plainData = com.sgtx.global.security.decrypt.AesDecryptoUtil.decrypt(envelope.getAesData(), aesKey);

            // 3. 해시 검증 (무결성 확인)
            boolean hashValid = com.sgtx.global.security.decrypt.HashDecryptoUtil.verifyHash(plainData, envelope.getItemHash());

            // 4. 전자서명 검증 (발신자: 판매자의 공개키 사용)
            UserEntity seller = trade.getSeller();
            java.security.PublicKey sellerPublicKey = com.sgtx.global.security.crypto.keyPairUtil.getPublicKeyFromString(seller.getPublicKey());
            boolean signatureValid = com.sgtx.global.security.decrypt.SignatureDecryptoUtil.verifySignature(envelope.getItemHash(), envelope.getSignature(), sellerPublicKey);

            if (!hashValid || !signatureValid) {
                log.error("🚨 [SECURITY BREACH] 위변조 감지! Hash Valid: {}, Signature Valid: {}", hashValid, signatureValid);
                trade.setStatus(TradeStatus.FAILED);
                throw new IllegalStateException("검증 실패: 데이터 위변조 또는 서명 불일치가 감지되었습니다.");
            }

            log.info("✅ [SECURITY VERIFIED] Trade ID {} 무결성 및 서명 검증 완료.", trade.getTradeId());
            trade.setStatus(TradeStatus.VERIFIED);

        } catch (Exception e) {
            log.error("❌ [ERROR] 검증 처리 중 오류 발생: {}", e.getMessage());
            throw new IllegalStateException("검증 과정에서 기술적 오류가 발생했습니다: " + e.getMessage());
        }

        return new TradeVerifyResponse(
                trade.getTradeId(),
                trade.getStatus().name(),
                trade.getBuyer().getUserId()
        );
    }

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
        trade.setPrice(request.price()); // 가격 설정

        // 최초 상태
        trade.setStatus(TradeStatus.PENDING);

        // DB 저장
        entityManager.persist(trade);
        try {
            String plainData =
                    "tradeId=" + trade.getTradeId()
                            + "|buyerId=" + buyer.getUserId()
                            + "|sellerId=" + seller.getUserId()
                            + "|itemId=" + item.getItemId()
                            + "|itemName=" + item.getItemName()
                            + "|price=" + trade.getPrice();

            SecretKey aesKey = AesCryptoUtil.generateAesKey();

            String encryptedData = AesCryptoUtil.encrypt(plainData, aesKey);

            PublicKey buyerPublicKey =
                    keyPairUtil.getPublicKeyFromString(
                            buyer.getPublicKey()
                    );

            String encryptedSessionKey =
                    RsaCryptoUtil.encryptAesKey(
                            aesKey,
                            buyerPublicKey
                    );

            String itemHash =
                    HashUtil.generateSHA1(plainData);

            if (seller.getPrivateKey() == null || seller.getPrivateKey().isBlank()) {
                throw new IllegalStateException("판매자의 개인키가 없어 전자서명을 생성할 수 없습니다.");
            }

            PrivateKey sellerPrivateKey =
                    keyPairUtil.getPrivateKeyFromString(seller.getPrivateKey());

            String signature =
                    SignatureUtil.createSignature(itemHash, sellerPrivateKey);

            EnvelopeEntity envelope = new EnvelopeEntity();
            envelope.setTrade(trade);
            envelope.setAesData(encryptedData);
            envelope.setRsaEncryptedKey(encryptedSessionKey);
            envelope.setSignature(signature);
            envelope.setItemHash(itemHash);

            envelopeRepository.save(envelope);

            return new TradeEnvelopeResponse(
                    trade.getTradeId(),
                    trade.getStatus().name(),
                    trade.getPrice(),
                    encryptedData,
                    encryptedSessionKey,
                    signature,
                    itemHash
            );

        } catch (Exception e) {
            throw new IllegalStateException("전자봉투 생성 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
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

        // [수정: 문제점 해결] .equals() 사용
        if (!trade.getBuyer().getUserId().equals(requestUserId) && !trade.getSeller().getUserId().equals(requestUserId)) {
            throw new UnauthorizedTradeAccessException("해당 거래를 취소할 권한이 없습니다.");
        }

        // 유효한 사유 검증
        if (!"HASH_MISMATCH".equals(reason) && !"SIGNATURE_INVALID".equals(reason)) {
            throw new IllegalArgumentException("올바르지 않은 취소 사유입니다.");
        }

        // 상태 확인
        if (trade.getStatus() == TradeStatus.COMPLETED) {
            throw new IllegalStateException("이미 완료된 거래입니다.");
        }

        // [보안 방어 로직: 강력한 로깅]
        log.error("🚨 [SECURITY BREACH DETECTED] Trade ID {} cancelled due to {}. Reported by User ID: {}. Expected Buyer: {}, Seller: {}", 
                  trade.getTradeId(), reason, requestUserId, trade.getBuyer().getUserId(), trade.getSeller().getUserId());

        // 거래 상태를 CANCELED로 변경
        trade.setStatus(TradeStatus.CANCELED);

        return new TradeCancelResponse(
            trade.getTradeId(),
            trade.getStatus().name(),
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

        // [수정: 문제점 해결] 결제 여부 확인 로직 추가 (결제 우회 방지)
        boolean isPaid = paymentRepository.existsByTradeId(trade.getTradeId());
        if (!isPaid) {
            throw new IllegalStateException("결제가 완료되지 않은 거래는 종료할 수 없습니다.");
        }

        if (trade.getStatus() != TradeStatus.VERIFIED) {
            throw new IllegalStateException("검증되지 않은 거래입니다.");
        }

        trade.setStatus(TradeStatus.COMPLETED);

        ItemEntity item = trade.getItem();
        if (item != null) {
            item.setOwner(trade.getBuyer());
        }

        return new TradeVerifyResponse(
                trade.getTradeId(),
                trade.getStatus().name(),
                trade.getBuyer().getUserId()
        );
    }
}
