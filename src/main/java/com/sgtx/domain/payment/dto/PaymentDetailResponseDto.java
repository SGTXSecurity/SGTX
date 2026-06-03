package com.sgtx.domain.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentDetailResponseDto {
    private Long paymentId;
    private Long tradeId;
    private Integer amount;
    private String paymentStatus;
    private CardInfo cardInfo;
    private LocalDateTime approvedAt;

    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CardInfo {
        private String cardCompany;
        private String maskedCardNumber;
    }
}
