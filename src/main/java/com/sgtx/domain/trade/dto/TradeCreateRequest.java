package com.sgtx.domain.trade.dto;

// 거래 생성 요청 DTO
public record TradeCreateRequest(

        Long buyerId,
        Long sellerId,
        Long itemId,
        Integer price,

        // AES 암호화 거래 문서
        String encryptedData,

        // RSA 공개키로 암호화한 AES 세션키
        String encryptedSessionKey,
        String signature,
        String itemHash
) {
}