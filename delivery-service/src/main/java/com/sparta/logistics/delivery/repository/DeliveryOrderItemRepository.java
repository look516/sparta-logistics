package com.sparta.logistics.delivery.repository;

import com.sparta.logistics.delivery.entity.DeliveryOrderItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Repository
public interface DeliveryOrderItemRepository extends JpaRepository<DeliveryOrderItemEntity, UUID> {
    List<DeliveryOrderItemEntity> findByDelivery_Id(UUID deliveryId);

    // afterCommit에서 호출 — 트랜잭션 외부이므로 직접 DELETE 쿼리로 처리
    @Modifying
    @Transactional
    @Query("DELETE FROM DeliveryOrderItemEntity d WHERE d.delivery.id = :deliveryId")
    void deleteByDelivery_Id(UUID deliveryId);
}