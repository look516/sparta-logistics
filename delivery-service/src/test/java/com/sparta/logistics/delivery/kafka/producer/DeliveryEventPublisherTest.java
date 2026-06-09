package com.sparta.logistics.delivery.kafka.producer;

import com.sparta.logistics.common.kafka.KafkaTopics;
import com.sparta.logistics.common.outbox.OutboxEventPublisher;
import com.sparta.logistics.delivery.entity.DeliveryOrderItemEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

/**
 * DeliveryEventPublisher 단위 테스트
 *
 * 각 publish 메서드가 OutboxEventPublisher.publish()를 올바른
 * topic / aggregateType="DELIVERY" 로 위임하는지 검증한다.
 * 실제 Kafka/DB 연결 없이 순수 단위 테스트로 수행된다.
 */
@ExtendWith(MockitoExtension.class)
class DeliveryEventPublisherTest {

    @Mock
    private OutboxEventPublisher outboxEventPublisher;

    private DeliveryEventPublisher publisher;

    private static final String AGGREGATE_TYPE = "DELIVERY";

    @BeforeEach
    void setUp() {
        publisher = new DeliveryEventPublisher(outboxEventPublisher);
    }

    // -----------------------------------------------------------------------
    // publishCreated
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("publishCreated → topic=DELIVERY_CREATED, aggregateType=DELIVERY, aggregateId=deliveryId")
    void publishCreated_delegatesToOutbox() {
        UUID deliveryId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();

        publisher.publishCreated(
                deliveryId, orderId,
                UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), 3,
                "서울시 강남구", 120,
                "U1234", "서울허브", "부산허브",
                LocalDateTime.now()
        );

        verify(outboxEventPublisher).publish(
                eq(KafkaTopics.DELIVERY_CREATED),
                eq(deliveryId.toString()),
                eq(AGGREGATE_TYPE),
                any()
        );
    }

    // -----------------------------------------------------------------------
    // publishCreationFailed
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("publishCreationFailed → topic=DELIVERY_CREATION_FAILED, aggregateId=orderId")
    void publishCreationFailed_delegatesToOutbox() {
        UUID orderId = UUID.randomUUID();
        UUID deliveryId = UUID.randomUUID();

        publisher.publishCreationFailed(orderId, deliveryId, "재고 부족", List.of());

        verify(outboxEventPublisher).publish(
                eq(KafkaTopics.DELIVERY_CREATION_FAILED),
                eq(orderId.toString()),
                eq(AGGREGATE_TYPE),
                any()
        );
    }

    // -----------------------------------------------------------------------
    // publishStarted
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("publishStarted → topic=DELIVERY_STARTED, aggregateId=deliveryId")
    void publishStarted_delegatesToOutbox() {
        UUID deliveryId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();

        publisher.publishStarted(deliveryId, orderId, List.of());

        verify(outboxEventPublisher).publish(
                eq(KafkaTopics.DELIVERY_STARTED),
                eq(deliveryId.toString()),
                eq(AGGREGATE_TYPE),
                any()
        );
    }

    // -----------------------------------------------------------------------
    // publishCancelledAck
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("publishCancelledAck → topic=DELIVERY_CANCELLED_ACK, aggregateId=orderId")
    void publishCancelledAck_delegatesToOutbox() {
        UUID deliveryId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();

        publisher.publishCancelledAck(deliveryId, orderId);

        verify(outboxEventPublisher).publish(
                eq(KafkaTopics.DELIVERY_CANCELLED_ACK),
                eq(orderId.toString()),
                eq(AGGREGATE_TYPE),
                any()
        );
    }

    // -----------------------------------------------------------------------
    // publishCancellationFailed
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("publishCancellationFailed → topic=DELIVERY_CANCELLATION_FAILED, aggregateId=orderId")
    void publishCancellationFailed_delegatesToOutbox() {
        UUID orderId = UUID.randomUUID();
        UUID deliveryId = UUID.randomUUID();

        publisher.publishCancellationFailed(orderId, deliveryId, "취소 불가 상태");

        verify(outboxEventPublisher).publish(
                eq(KafkaTopics.DELIVERY_CANCELLATION_FAILED),
                eq(orderId.toString()),
                eq(AGGREGATE_TYPE),
                any()
        );
    }
}
