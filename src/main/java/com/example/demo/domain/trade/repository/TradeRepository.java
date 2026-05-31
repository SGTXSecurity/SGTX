package com.example.demo.domain.trade.repository;

import com.example.demo.domain.trade.entity.TradeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TradeRepository extends JpaRepository<TradeEntity, Long> {
}
