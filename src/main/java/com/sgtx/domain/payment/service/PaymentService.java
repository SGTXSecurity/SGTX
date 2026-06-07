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
import com.sgtx.global.exception.UnauthorizedTradeAccessException;
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

    @Transactional (rollbackFor = Exception.class)
    public PaymentResponseDto processPayment(PaymentRequestDto request, Long currentUserId) {

        log.info("결제중: tradeId={}, cardId={}, amount={}",
                request.getTradeId(), request.getCardId(), request.getAmount());

        TradeEntity trade = tradeRepository.findByIdWithLock(request.getTradeId())
                .orElseThrow(() -> new TradeNotFoundException("거래 조회 실패: " + request.getTradeId()));

        if (trade.getBuyer() == null || !trade.getBuyer().getUserId().equals(currentUserId)){
            log.error("권한 없는 사용자의 결제 시도");
            throw new UnauthorizedTradeAccessException("해당 결제에 접근할 권한이 없습니다.");
        }

        if (trade.getStatus() != TradeStatus.VERIFIED) {
            log.error("보안 문제: 검증되지 않은 거래에 대한 결제 시도");
            throw new IllegalStateException("보안 검증이 완료되지 않은 거래는 결제할 수 없습니다.");
        }
        if (trade.getStatus() == TradeStatus.COMPLETED){
            throw new IllegalStateException("이미 결제가 완료된 거래입니다.");
        }

        if (!trade.getPrice().equals(request.getAmount())) {
            throw new PaymentAmountMismatchException("요청된 결제 금액(" + request.getAmount() + ")이 실제 거래 금액(" + trade.getPrice() + ")과 일치하지 않습니다.");
        }

        // 가상의 카드사 API 호출 (성공 가정)
        boolean isCardApproved = true; 
        if (!isCardApproved) {
            throw new PaymentFailedException("카드사 승인이 거절되었습니다. (사유: 한도 초과)", "PAY_002");
        }

        PaymentEntity payment = new PaymentEntity();
        payment.setTradeId(trade.getTradeId());
        payment.setCardId(request.getCardId());
        payment.setAmount(request.getAmount());
        payment.setPaymentStatus(PaymentStatus.SUCCESS);
        payment.setApprovedAt(LocalDateTime.now());
        paymentRepository.save(payment);

        trade.setStatus(TradeStatus.COMPLETED);

        if (trade.getItem() != null) {
            trade.getItem().setOwner(trade.getBuyer());
        }
        
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

        TradeEntity trade = tradeRepository.findById(payment.getTradeId())
                .orElseThrow(() -> new TradeNotFoundException("관련 거래 정보를 찾을 수 없습니다."));

        if (!trade.getBuyer().getUserId().equals(currentUserId)){
            throw new UnauthorizedTradeAccessException("해당 정보에 접금할 권한이 없습니다.");
        }

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
