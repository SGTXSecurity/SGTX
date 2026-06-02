package com.sgtx.domain.envelope.dto;

// 전자봉투 저장 요청 DTO
public record EnvelopeRequestDto(

        Long tradeId,

        // AES 암호화 거래 문서
        String aesData,

        // RSA 공개키로 암호화한 AES 세션키
        String rsaEncryptedKey,
        String signature,
        String itemHash
) {
}
