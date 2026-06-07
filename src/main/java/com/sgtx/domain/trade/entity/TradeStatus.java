package com.sgtx.domain.trade.entity;

public enum TradeStatus {
    PENDING("대기중"),
    VERIFIED("검증완료"),
    PAID("결제완료"),
    COMPLETED("거래완료"),
    CANCELED("거래취소"),
    FAILED("실패");

    private final String description;

    TradeStatus(String description) {
        this.description = description;
    }
    public String getDescription() {
        return description;
    }
}
