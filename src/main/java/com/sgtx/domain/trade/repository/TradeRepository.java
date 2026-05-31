package com.sgtx.domain.trade.repository;

import com.sgtx.domain.trade.entity.TradeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TradeRepository extends JpaRepository<TradeEntity, Long> {
}
