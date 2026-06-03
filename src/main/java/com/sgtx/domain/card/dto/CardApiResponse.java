package com.sgtx.domain.card.dto;

//공통 응답 DTO
public record CardApiResponse<T>(
        int status,
        String message,
        T data
) {
}