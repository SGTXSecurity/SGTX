package com.example.demo.domain.item.repository;

import com.example.demo.domain.item.entity.ItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ItemRepository extends JpaRepository<ItemEntity, Long> {
}
