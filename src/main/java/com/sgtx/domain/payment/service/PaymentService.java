package com.sgtx.domain.payment.service;

import com.sgtx.domain.card.entity.CardEntity;
import com.sgtx.domain.card.repository.CardRepository;
import com.sgtx.domain.payment.dto.PaymentDetailResponseDto;
import com.sgtx.domain.payment.dto.PaymentRequestDto;
import com.sgtx.domain.payment.dto.PaymentResponseDto;
import com.sgtx.domain.payment.entity.PaymentEntity;
import com.sgtx.domain.payment.entity.PaymentStatus;
import com.sgtx.domain.payment.repository.PaymentRepository;
import com.sgtx.domain.trade.entity.TradeEntity;
import com.sgtx.domain.trade.entity.TradeStatus;
import com.sgtx.domain.trade.repository.TradeRepository;
import com.sgtx.global.exception.PaymentAmountMismatchException;
import com.sgtx.global.exception.PaymentFailedException;
import com.sgtx.global.exception.TradeNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final TradeRepository tradeRepository;
    private final CardRepository cardRepository;

    // [보안 취약점 1: 트랜잭션 관리 미흡 (CWE-209)] 
    @Transactional
    public PaymentResponseDto processPayment(PaymentRequestDto request, Long currentUserId) {

        log.info("Processing payment: tradeId={}, cardId={}, amount={}", 
                request.getTradeId(), request.getCardId(), request.getAmount());

        TradeEntity trade = tradeRepository.findById(request.getTradeId())
                .orElseThrow(() -> new TradeNotFoundException("Trade not found: " + request.getTradeId()));

        // [보안 취약점 3: Insecure Direct Object Reference (IDOR) (CWE-639)]
        // (IDOR 취약점 유지)

        // [보안 취약점 4: 비즈니스 로직 검증 취약 - 참조 비교 (CWE-697)]
        if (trade.getPrice() != request.getAmount()) {
            throw new PaymentAmountMismatchException("요청된 결제 금액(" + request.getAmount() + ")이 실제 거래 금액(" + trade.getPrice() + ")과 일치하지 않습니다.");
        }

        // 가상의 카드사 API 호출 (성공 가정)
        boolean isCardApproved = true; 
        if (!isCardApproved) {
            throw new PaymentFailedException("카드사 승인이 거절되었습니다. (사유: 한도 초과)", "PAY_002");
        }

        // 결제 정보 저장 (Entity 리팩토링 없이 cardId만 저장)
        PaymentEntity payment = new PaymentEntity();
        payment.setTradeId(trade.getTradeId());
        payment.setCardId(request.getCardId());
        payment.setAmount(request.getAmount());
        payment.setPaymentStatus(PaymentStatus.SUCCESS);
        payment.setApprovedAt(LocalDateTime.now());
        paymentRepository.save(payment);

        // 거래 상태 업데이트
        trade.setStatus(TradeStatus.COMPLETED);
        tradeRepository.save(trade);

        return PaymentResponseDto.builder()
                .paymentId(payment.getPaymentId())
                .tradeId(payment.getTradeId())
                .amount(payment.getAmount())
                .paymentStatus(payment.getPaymentStatus().name())
                .approvedAt(payment.getApprovedAt())
                .build();
    }

    @Transactional(readOnly = true)
    public PaymentDetailResponseDto getPaymentDetail(Long paymentId, Long currentUserId) {
        
        PaymentEntity payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("해당 결제 내역을 찾을 수 없습니다."));

        CardEntity card = cardRepository.findById(payment.getCardId())
                .orElseThrow(() -> new RuntimeException("카드 정보를 찾을 수 없습니다."));

        return PaymentDetailResponseDto.builder()
                .paymentId(payment.getPaymentId())
                .tradeId(payment.getTradeId())
                .amount(payment.getAmount())
                .paymentStatus(payment.getPaymentStatus().name())
                .cardInfo(new PaymentDetailResponseDto.CardInfo(
                        card.getCardCompany(),
                        card.getMaskedCardNumber()
                ))
                .approvedAt(payment.getApprovedAt())
                .build();
    }
}
