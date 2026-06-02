package com.sgtx.global.exception;

import com.sgtx.global.common.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(TradeNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleTradeNotFound(TradeNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(404, e.getMessage(), "TRADE_002"));
    }

    @ExceptionHandler(UnauthorizedTradeAccessException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorizedAccess(UnauthorizedTradeAccessException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse(403, e.getMessage(), "TRADE_001"));
    }

    @ExceptionHandler(EnvelopeNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleEnvelopeNotFound(EnvelopeNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(404, e.getMessage(), "ENVELOPE_002"));
    }

    @ExceptionHandler(EnvelopeAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleEnvelopeAlreadyExists(EnvelopeAlreadyExistsException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(409, e.getMessage(), "ENVELOPE_001"));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalState(IllegalStateException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(400, e.getMessage(), "TRADE_003"));
    }

    // [보안 취약점] 애플리케이션 내부 구조(Stack Trace 등)를 클라이언트에게 그대로 노출
    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleGeneralException(Exception e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Internal Server Error Details: " + e.toString());
    }
}
