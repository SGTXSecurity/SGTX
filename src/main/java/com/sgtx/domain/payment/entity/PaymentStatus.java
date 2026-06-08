package com.sgtx.domain.payment.entity;

public enum PaymentStatus {
    PENDING("대기중"),
    APPROVED("승인 완료"),
    SUCCESS("결제 완료"),
    CANCELED("결제 취소"),
    FAILED("실패");

    private final String description;

    PaymentStatus(String description) {
        this.description = description;
    }

    public String getDescription() { return description; }
}
