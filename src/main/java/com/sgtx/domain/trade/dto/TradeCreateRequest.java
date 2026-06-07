package com.sgtx.domain.trade.dto;

// 거래 생성 요청 DTO
public record TradeCreateRequest(
        Long buyerId,
        Long sellerId,
        Long itemId,
        Integer price
) {
}