package com.sgtx.domain.envelope.service;

import com.sgtx.domain.envelope.dto.EnvelopeRequestDto;
import com.sgtx.domain.envelope.dto.EnvelopeResponseDto;
import com.sgtx.domain.envelope.entity.EnvelopeEntity;
import com.sgtx.domain.envelope.repository.EnvelopeRepository;
import com.sgtx.domain.trade.entity.TradeEntity;
import com.sgtx.domain.trade.repository.TradeRepository;
import com.sgtx.global.exception.EnvelopeAlreadyExistsException;
import com.sgtx.global.exception.EnvelopeNotFoundException;
import com.sgtx.global.exception.TradeNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EnvelopeService {

    private final EnvelopeRepository envelopeRepository;
    private final TradeRepository tradeRepository;

    // 전자봉투 저장
    @Transactional
    public EnvelopeResponseDto createEnvelope(EnvelopeRequestDto request) {

        // 거래 존재 여부 확인
        TradeEntity trade = tradeRepository.findById(request.tradeId())
                .orElseThrow(() -> new TradeNotFoundException(
                        "존재하지 않는 거래입니다. tradeId: " + request.tradeId()));

        // trade_id 는 UNIQUE 이므로 중복 저장 방지
        if (envelopeRepository.existsByTrade_TradeId(request.tradeId())) {
            throw new EnvelopeAlreadyExistsException(
                    "이미 전자봉투가 존재하는 거래입니다. tradeId: " + request.tradeId());
        }

        EnvelopeEntity envelope = new EnvelopeEntity();
        envelope.setTrade(trade);
        envelope.setAesData(request.aesData());
        envelope.setRsaEncryptedKey(request.rsaEncryptedKey());
        envelope.setSignature(request.signature());
        envelope.setItemHash(request.itemHash());

        EnvelopeEntity saved = envelopeRepository.save(envelope);

        return toResponse(saved);
    }

    // tradeId 기준 전자봉투 조회
    @Transactional(readOnly = true)
    public EnvelopeResponseDto getEnvelope(Long tradeId) {

        EnvelopeEntity envelope = envelopeRepository.findByTrade_TradeId(tradeId)
                .orElseThrow(() -> new EnvelopeNotFoundException(
                        "해당 거래의 전자봉투를 찾을 수 없습니다. tradeId: " + tradeId));

        return toResponse(envelope);
    }

    private EnvelopeResponseDto toResponse(EnvelopeEntity envelope) {
        return new EnvelopeResponseDto(
                envelope.getEnvelopeId(),
                envelope.getTrade().getTradeId(),
                envelope.getAesData(),
                envelope.getRsaEncryptedKey(),
                envelope.getSignature(),
                envelope.getItemHash(),
                envelope.getCreatedAt()
        );
    }
}