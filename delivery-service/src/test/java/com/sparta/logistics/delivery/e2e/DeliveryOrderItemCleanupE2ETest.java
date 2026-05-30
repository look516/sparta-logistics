package com.sparta.logistics.delivery.e2e;

import com.sparta.logistics.delivery.client.HubServiceClient;
import com.sparta.logistics.delivery.client.UserServiceClient;
import com.sparta.logistics.delivery.entity.DeliveryEntity;
import com.sparta.logistics.delivery.entity.DeliveryOrderItemEntity;
import com.sparta.logistics.delivery.repository.DeliveryOrderItemRepository;
import com.sparta.logistics.delivery.repository.DeliveryRepository;
import com.sparta.logistics.delivery.service.DeliveryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

// 1931ed3 회귀 방지 — delivery.started 발행 후 DeliveryOrderItem 임시 데이터 삭제
// afterCommit은 트랜잭션 커밋 후 동기적으로 실행되므로 실제 Kafka 브로커 필요
@SpringBootTest
@ActiveProfiles("test")
@EmbeddedKafka(partitions = 1, topics = {"delivery.started", "delivery.created", "ai.deadline.calculated", "cancel.delivery.command"})
class DeliveryOrderItemCleanupE2ETest {

    @Autowired DeliveryService deliveryService;
    @Autowired DeliveryRepository deliveryRepository;
    @Autowired DeliveryOrderItemRepository orderItemRepository;

    @MockBean HubServiceClient hubServiceClient;
    @MockBean UserServiceClient userServiceClient;

    @Test
    void updateFinalDispatchDeadline_호출_후_OrderItem_삭제됨() {
        // given
        DeliveryEntity delivery = new DeliveryEntity(
                UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), UUID.randomUUID(), "주소", "slack");
        deliveryRepository.save(delivery);

        DeliveryOrderItemEntity item = new DeliveryOrderItemEntity(
                delivery, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), 3);
        orderItemRepository.save(item);

        assertThat(orderItemRepository.findByDelivery_Id(delivery.getId())).hasSize(1);

        // when — afterCommit이 동기적으로 실행됨: publishStarted 후 deleteByDelivery_Id 호출
        deliveryService.updateFinalDispatchDeadline(delivery.getId(), LocalDateTime.now());

        // then
        List<DeliveryOrderItemEntity> remaining =
                orderItemRepository.findByDelivery_Id(delivery.getId());
        assertThat(remaining).isEmpty();
    }
}
