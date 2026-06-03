package com.sgtx.global.exception;

public class PaymentFailedException extends RuntimeException {
    private final String code;
    public PaymentFailedException(String message, String code) {
        super(message);
        this.code = code;
    }
    public String getCode() { return code; }
}
