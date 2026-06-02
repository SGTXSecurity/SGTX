package com.sgtx.domain.envelope.repository;

import com.sgtx.domain.envelope.entity.EnvelopeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EnvelopeRepository extends JpaRepository<EnvelopeEntity, Long> {

    // 거래 ID(trade_id) 기준 전자봉투 조회
    Optional<EnvelopeEntity> findByTrade_TradeId(Long tradeId);

    // 거래 ID(trade_id) 기준 전자봉투 존재 여부 확인
    boolean existsByTrade_TradeId(Long tradeId);
}
