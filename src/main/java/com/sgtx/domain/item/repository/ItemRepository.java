package com.sgtx.domain.item.repository;

import com.sgtx.domain.item.entity.ItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ItemRepository extends JpaRepository<ItemEntity, Long> {
}
