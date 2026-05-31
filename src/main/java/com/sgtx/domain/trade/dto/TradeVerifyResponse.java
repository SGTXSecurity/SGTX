package com.sgtx.domain.trade.dto;

public record TradeVerifyResponse(
    Long tradeId,
    String status,
    Long itemOwnerId
) {}
