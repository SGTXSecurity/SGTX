package com.sgtx.domain.card.repository;

import com.sgtx.domain.card.entity.CardEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CardRepository extends JpaRepository<CardEntity, Long> {

    // 특정 사용자(user_id)가 등록한 카드 목록 조회
    List<CardEntity> findByUser_UserId(Long userId);
}
