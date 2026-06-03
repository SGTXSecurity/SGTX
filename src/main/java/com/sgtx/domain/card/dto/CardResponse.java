package com.sgtx.domain.card.dto;

import com.sgtx.domain.card.CardStatus;

import java.time.LocalDateTime;

// 카드 조회 응답 DTO (원본 카드 번호는 포함하지 않는다)
public record CardResponse(
        Long cardId,
        String cardCompany,
        String maskedCardNumber,
        LocalDateTime createdAt
) {
}
