package com.sgtx.domain.envelope.dto;

import java.time.LocalDateTime;

// 전자봉투 조회 응답 DTO
public record EnvelopeResponseDto(
        Long envelopeId,
        Long tradeId,
        String aesData,
        String rsaEncryptedKey,
        String signature,
        String itemHash,
        LocalDateTime createdAt
) {
}
