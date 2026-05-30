package com.sparta.logistics.delivery.repository;

import com.sparta.logistics.delivery.entity.DeliveryOrderItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Repository
public interface DeliveryOrderItemRepository extends JpaRepository<DeliveryOrderItemEntity, UUID> {
    List<DeliveryOrderItemEntity> findByDelivery_Id(UUID deliveryId);

    // afterCommit에서 호출 — 트랜잭션 외부이므로 REQUIRED로 새 트랜잭션 시작
    @Transactional
    void deleteByDelivery_Id(UUID deliveryId);
}