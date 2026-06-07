package com.sgtx.domain.trade.dto;

public record TradeEnvelopeResponse(
    Long tradeId,
    String status,
    Integer price,
    String encryptedData,
    String encryptedSessionKey,
    String signature,
    String itemHash
) {}
