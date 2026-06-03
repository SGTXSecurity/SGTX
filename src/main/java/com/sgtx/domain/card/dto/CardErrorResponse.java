package com.sgtx.domain.card.dto;

public record CardErrorResponse(
    int status,
    String message,
    String code
) {
}