package com.sgtx.global.exception;

public class UnauthorizedTradeAccessException extends RuntimeException {
    public UnauthorizedTradeAccessException(String message) {
        super(message);
    }
}
