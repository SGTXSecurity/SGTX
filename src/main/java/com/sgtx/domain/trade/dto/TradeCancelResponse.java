package com.sgtx.domain.trade.dto;

import java.time.LocalDateTime;

public record TradeCancelResponse(
    Long tradeId,
    String status,
    String reason,
    LocalDateTime canceledAt
) {}
