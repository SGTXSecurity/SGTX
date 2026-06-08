package com.sgtx.domain.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRequestDto {
    private Long tradeId;
    private Long cardId;
    private String cardNumber; // 가상 카드사 전송을 위한 원본 카드 번호
    private Integer amount;
}
