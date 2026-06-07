package com.sgtx.domain.trade.dto;

import java.time.LocalDateTime;

//거래 취소 반환
public record TradeCancelResponse(
    Long tradeId,
    String status,
    String reason,
    LocalDateTime canceledAt
) {}
