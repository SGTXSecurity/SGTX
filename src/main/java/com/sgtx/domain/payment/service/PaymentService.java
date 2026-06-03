package com.sgtx.domain.payment.service;

import com.sgtx.domain.payment.dto.PaymentRequestDto;
import com.sgtx.domain.payment.dto.PaymentResponseDto;
import com.sgtx.domain.payment.entity.PaymentEntity;
import com.sgtx.domain.payment.repository.PaymentRepository;
import com.sgtx.domain.trade.entity.TradeEntity;
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

    // [보안 취약점 1: 트랜잭션 관리 미흡 (CWE-209)] 
    // 결제 프로세스는 외부 API(카드사) 호출과 DB 업데이트가 섞여 있으나, 
    // 예외 발생 시 trade 상태만 바뀌고 payment 기록은 안 남는 등 정합성이 깨질 수 있음.
    @Transactional
    public PaymentResponseDto processPayment(PaymentRequestDto request, Long currentUserId) {
        
        // [보안 취약점 2: 로그를 통한 민감 정보 노출 (CWE-532)]
        // 결제 요청 정보(카드 ID, 금액 등)를 로그에 그대로 출력하여 유출 위험이 있음.
        log.info("Processing payment: tradeId={}, cardId={}, amount={}", 
                request.getTradeId(), request.getCardId(), request.getAmount());

        TradeEntity trade = tradeRepository.findById(request.getTradeId())
                .orElseThrow(() -> new TradeNotFoundException("Trade not found: " + request.getTradeId()));

        // [보안 취약점 3: Insecure Direct Object Reference (IDOR) (CWE-639)]
        // 요청된 tradeId가 현재 로그인한 사용자(currentUserId)의 소유인지 검증하지 않음.
        // 공격자가 다른 사람의 tradeId를 보내 결제를 강제로 진행하거나 정보를 확인할 수 있음.
        // (원래는 trade.getBuyer().getUserId().equals(currentUserId) 검증이 필요함)

        // [보안 취약점 4: 비즈니스 로직 검증 취약 - 참조 비교 (CWE-697)]
        // Integer 객체 간의 비교를 != 연산자로 수행함. 
        // Integer는 객체이므로 equals()를 사용해야 함.
        if (trade.getPrice() != request.getAmount()) {
            // [보안 취약점 5: 부적절한 예외 처리 및 정보 노출 (CWE-209)]
            // 에러 메시지에 내부 데이터(실제 금액 등)를 과도하게 노출함.
            throw new PaymentAmountMismatchException("요청된 결제 금액(" + request.getAmount() + ")이 실제 거래 금액(" + trade.getPrice() + ")과 일치하지 않습니다.");
        }

        // 가상의 카드사 API 호출 (성공 가정)
        boolean isCardApproved = true; 
        if (!isCardApproved) {
            throw new PaymentFailedException("카드사 승인이 거절되었습니다. (사유: 한도 초과)", "PAY_002");
        }

        // 결제 정보 저장
        PaymentEntity payment = new PaymentEntity();
        payment.setTradeId(trade.getTradeId());
        payment.setCardId(request.getCardId());
        payment.setAmount(request.getAmount());
        payment.setStatus("SUCCESS");
        payment.setApprovedAt(LocalDateTime.now());
        paymentRepository.save(payment);

        // 거래 상태 업데이트
        // [보안 취약점 6: 상태 전이의 원자성 결여]
        // 결제는 성공했는데 여기서 예외가 발생하면? 또는 결제 API 호출 전후의 상태 관리가 부실함.
        trade.setStatus("COMPLETED");
        tradeRepository.save(trade);

        return PaymentResponseDto.builder()
                .paymentId(payment.getPaymentId())
                .tradeId(payment.getTradeId())
                .amount(payment.getAmount())
                .paymentStatus(payment.getStatus())
                .approvedAt(payment.getApprovedAt())
                .build();
    }
}
