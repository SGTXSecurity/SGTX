package com.sgtx.global.common;

public record ErrorResponse(
    int status,
    String message,
    String code
) {}
