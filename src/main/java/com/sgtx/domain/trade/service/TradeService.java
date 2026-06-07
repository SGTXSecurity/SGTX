package com.sgtx.domain.trade.service;

import com.sgtx.domain.envelope.entity.EnvelopeEntity;
import com.sgtx.domain.envelope.repository.EnvelopeRepository;
import com.sgtx.domain.item.entity.ItemEntity;
import com.sgtx.domain.trade.dto.*;
import com.sgtx.domain.trade.entity.TradeEntity;
import com.sgtx.domain.trade.entity.TradeStatus;
import com.sgtx.domain.trade.repository.TradeRepository;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@RequiredArgsConstructor
public class TradeService {
    private final EntityManager entityManager;
    private final EnvelopeRepository envelopeRepository;
    private final PaymentRepository paymentRepository;
    private final TradeRepository tradeRepository;
    private final com.sgtx.domain.envelope.service.EnvelopeService envelopeService;

    private static final Logger log = LoggerFactory.getLogger(TradeService.class);

    @Transactional
    public TradeEnvelopeResponse getTradeEnvelope(String tradeId, Long requestUserId) {
        TradeEntity trade = tradeRepository.findById(Long.parseLong(tradeId))
            .orElseThrow(() -> new TradeNotFoundException("존재하지 않는 거래입니다."));

        if(!trade.getBuyer().getUserId().equals(requestUserId)){
            log.error("보안 문제 발생");
            throw new UnauthorizedTradeAccessException("이 거래를 조회할 권한이 없습니다.");
        }

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

    // 전자봉투 검증
    @Transactional
    public TradeVerifyResponse verifyTrade(String tradeId, Long requestUserId) {
        TradeEntity trade = tradeRepository.findById(Long.parseLong(tradeId))
                .orElseThrow(() -> new TradeNotFoundException("존재하지 않는 거래입니다."));

        if (!trade.getBuyer().getUserId().equals(requestUserId)) {
            throw new UnauthorizedTradeAccessException("해당 거래를 완료할 권한이 없습니다.");
        }

        // 상태 확인
        if (trade.getStatus() == TradeStatus.COMPLETED || trade.getStatus() == TradeStatus.PAID || trade.getStatus() == TradeStatus.CANCELED) {
            throw new IllegalStateException("이미 결제/완료되었거나 취소된 거래는 다시 검증할 수 없습니다.");
        }

        EnvelopeEntity envelope = envelopeRepository
                .findByTrade_TradeId(Long.parseLong(tradeId))
                .orElseThrow(() -> new EnvelopeNotFoundException("전자봉투를 찾을 수 없습니다."));

        try {
            // RSA 개인키로 AES 세션키 복호화 (수신자: 구매자의 개인키 사용)
            UserEntity buyer = trade.getBuyer();
            if (buyer.getPrivateKey() == null) {
                throw new IllegalStateException("구매자의 개인키가 존재하지 않아 복호화할 수 없습니다.");
            }
            java.security.PrivateKey buyerPrivateKey = com.sgtx.global.security.crypto.keyPairUtil.getPrivateKeyFromString(buyer.getPrivateKey());
            javax.crypto.SecretKey aesKey = com.sgtx.global.security.decrypt.RsaDecryptoUtil.decryptAesKey(envelope.getRsaEncryptedKey(), buyerPrivateKey);

            // AES 세션키로 암호화된 데이터 복호화
            String plainData = com.sgtx.global.security.decrypt.AesDecryptoUtil.decrypt(envelope.getAesData(), aesKey);

            // 해시 검증 (무결성 확인)
            boolean hashValid = com.sgtx.global.security.decrypt.HashDecryptoUtil.verifyHash(plainData, envelope.getItemHash());

            // 전자서명 검증 (발신자: 판매자의 공개키 사용)
            UserEntity seller = trade.getSeller();
            java.security.PublicKey sellerPublicKey = com.sgtx.global.security.crypto.keyPairUtil.getPublicKeyFromString(seller.getPublicKey());
            boolean signatureValid = com.sgtx.global.security.decrypt.SignatureDecryptoUtil.verifySignature(envelope.getItemHash(), envelope.getSignature(), sellerPublicKey);

            if (!hashValid || !signatureValid) {
                log.error("위변조 감지");
                trade.setStatus(TradeStatus.FAILED);
                throw new IllegalStateException("검증 실패: 데이터 위변조 또는 서명 불일치가 감지되었습니다.");
            }

            log.info("무결성 및 서명 검증 완료");
            trade.setStatus(TradeStatus.VERIFIED);

        } catch (Exception e) {
            log.error("검증 처리 중 오류 발생");
            throw new IllegalStateException("검증 과정에서 기술적 오류가 발생했습니다: " + e.getMessage());
        }

        return new TradeVerifyResponse(
                trade.getTradeId(),
                trade.getStatus().name(),
                trade.getBuyer().getUserId()
        );
    }

    //거래 시작(전자봉투 생성)
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

            //2. AES 키 생성 & 암호화
            SecretKey aesKey = AesCryptoUtil.generateAesKey();
            String encryptedData = AesCryptoUtil.encrypt(plainData, aesKey);

            PublicKey buyerPublicKey =
                    keyPairUtil.getPublicKeyFromString(
                            buyer.getPublicKey()
                    );

            //3, RSA (구매자 공개키 -> 암호화)
            String encryptedSessionKey =
                    RsaCryptoUtil.encryptAesKey(
                            aesKey,
                            buyerPublicKey
                    );

            //4. 해시
            String itemHash =
                    HashUtil.generateSHA1(plainData);

            if (seller.getPrivateKey() == null || seller.getPrivateKey().isBlank()) {
                throw new IllegalStateException("판매자의 개인키가 없어 전자서명을 생성할 수 없습니다.");
            }

            PrivateKey sellerPrivateKey =
                    keyPairUtil.getPrivateKeyFromString(seller.getPrivateKey());

            //5. 전자서명
            String signature =
                    SignatureUtil.createSignature(itemHash, sellerPrivateKey);

            com.sgtx.domain.envelope.dto.EnvelopeRequestDto envelopeRequest = 
                new com.sgtx.domain.envelope.dto.EnvelopeRequestDto(
                    trade.getTradeId(),
                    encryptedData,
                    encryptedSessionKey,
                    signature,
                    itemHash
            );

            envelopeService.createEnvelope(envelopeRequest);

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
            throw new IllegalStateException("전자봉투 생성 중 오류가 발생했습니다: ");
        }
    }

    @Transactional
    public TradeCancelResponse cancelTradeForSecurity(String tradeId, String reason, Long requestUserId) {
        TradeEntity trade = tradeRepository.findById(Long.parseLong(tradeId))
                .orElseThrow(() -> new TradeNotFoundException("존재하지 않는 거래입니다."));

        // .equals() 사용
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

        TradeEntity trade = tradeRepository.findById(Long.parseLong(tradeId))
                .orElseThrow(() -> new TradeNotFoundException("존재하지 않는 거래입니다."));

        // 권한 확인
        if (!trade.getBuyer().getUserId().equals(requestUserId)) {
            throw new UnauthorizedTradeAccessException("해당 거래를 종료할 권한이 없습니다.");
        }

        // 결제 여부 확인
        boolean isPaid = paymentRepository.existsByTradeId(trade.getTradeId());
        if (!isPaid || trade.getStatus() != TradeStatus.PAID) {
            throw new IllegalStateException("결제가 완료되지 않은 거래는 종료할 수 없습니다.");
        }

        // 최종 거래 완료 처리
        trade.setStatus(TradeStatus.COMPLETED);

        // 아이템 소유권 이전
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
