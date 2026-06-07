package com.sgtx.domain.trade.repository;

import com.sgtx.domain.trade.entity.TradeEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface TradeRepository extends JpaRepository<TradeEntity, Long> {
    // 트랜잭션 관리를 위한 비관적 락 도입
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from TradeEntity t where t.tradeId = :id")
    Optional<TradeEntity> findByIdWithLock(@Param("id") Long id);
}
