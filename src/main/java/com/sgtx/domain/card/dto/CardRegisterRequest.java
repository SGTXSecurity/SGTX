package com.sgtx.domain.card.dto;

// 카드 등록 요청 DTO
public record CardRegisterRequest(
        Long userId,
        String cardCompany,

        // 원본 카드 번호 (DB 에는 마스킹 후 저장, 응답에는 절대 노출하지 않는다)
        String cardNumber
) {
}
