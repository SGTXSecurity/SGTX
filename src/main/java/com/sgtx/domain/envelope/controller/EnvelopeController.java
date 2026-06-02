package com.sgtx.domain.envelope.controller;

import com.sgtx.domain.envelope.dto.EnvelopeRequestDto;
import com.sgtx.domain.envelope.dto.EnvelopeResponseDto;
import com.sgtx.domain.envelope.service.EnvelopeService;
import com.sgtx.global.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/envelopes")
@RequiredArgsConstructor
public class EnvelopeController {

    private final EnvelopeService envelopeService;

    // 전자봉투 저장
    @PostMapping
    public ResponseEntity<ApiResponse<EnvelopeResponseDto>> createEnvelope(
            @RequestBody EnvelopeRequestDto request) {

        EnvelopeResponseDto response = envelopeService.createEnvelope(request);

        return ResponseEntity.ok(
                ApiResponse.success("전자봉투 저장 성공", response)
        );
    }

    // tradeId 기준 전자봉투 조회
    @GetMapping("/{tradeId}")
    public ResponseEntity<ApiResponse<EnvelopeResponseDto>> getEnvelope(
            @PathVariable Long tradeId) {

        EnvelopeResponseDto response = envelopeService.getEnvelope(tradeId);

        return ResponseEntity.ok(
                ApiResponse.success("전자봉투 조회 성공", response)
        );
    }
}
