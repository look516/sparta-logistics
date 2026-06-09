package com.sparta.logistics.delivery.kafka.producer;

import com.sparta.logistics.common.kafka.KafkaTopics;
import com.sparta.logistics.common.kafka.event.DeliveryCancellationFailedEvent;
import com.sparta.logistics.common.kafka.event.DeliveryCancelledAckEvent;
import com.sparta.logistics.common.kafka.event.DeliveryCreatedEvent;
import com.sparta.logistics.common.kafka.event.DeliveryCreationFailedEvent;
import com.sparta.logistics.common.kafka.event.DeliveryOrderItemPayload;
import com.sparta.logistics.common.kafka.event.DeliveryStartedEvent;
import com.sparta.logistics.common.kafka.event.RestoreStockItemPayload;
import com.sparta.logistics.common.outbox.OutboxEventPublisher;
import com.sparta.logistics.delivery.entity.DeliveryOrderItemEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Delivery 도메인 이벤트를 Outbox 테이블에 저장함
 * 실제 Kafka 발행은 OutboxEventRelay가 담당함 (at-least-once 보장)
 **/
@Slf4j
@Component
@RequiredArgsConstructor
public class DeliveryEventPublisher {

    private final OutboxEventPublisher outboxEventPublisher;

    public void publishCreated(UUID deliveryId, UUID orderId,
                               UUID sourceHubId, UUID destinationHubId,
                               UUID companyDeliveryManagerId, int totalDeliveryCount,
                               String deliveryAddress, int totalEstimatedDuration,
                               String receiverSlackId, String sourceHubName, String destinationHubName,
                               LocalDateTime createdAt) {
        DeliveryCreatedEvent event = DeliveryCreatedEvent.builder()
                .eventId(UUID.randomUUID())
                .deliveryId(deliveryId)
                .orderId(orderId)
                .sourceHubId(sourceHubId)
                .destinationHubId(destinationHubId)
                .companyDeliveryManagerId(companyDeliveryManagerId)
                .totalDeliveryCount(totalDeliveryCount)
                .deliveryAddress(deliveryAddress)
                .totalEstimatedDuration(totalEstimatedDuration)
                .receiverSlackId(receiverSlackId)
                .sourceHubName(sourceHubName)
                .destinationHubName(destinationHubName)
                .createdAt(createdAt)
                .build();

        outboxEventPublisher.publish(KafkaTopics.DELIVERY_CREATED, deliveryId.toString(), "DELIVERY", event);
        log.info("[Outbox] delivery.created 저장 deliveryId={} orderId={}", deliveryId, orderId);
    }

    public void publishCreationFailed(UUID orderId, UUID deliveryId,
                                      String reason, List<RestoreStockItemPayload> itemsToRestore) {
        DeliveryCreationFailedEvent event = DeliveryCreationFailedEvent.builder()
                .eventId(UUID.randomUUID())
                .orderId(orderId)
                .deliveryId(deliveryId)
                .reason(reason)
                .itemsToRestore(itemsToRestore)
                .build();

        outboxEventPublisher.publish(KafkaTopics.DELIVERY_CREATION_FAILED, orderId.toString(), "DELIVERY", event);
        log.info("[Outbox] delivery.creation.failed 저장 orderId={} reason={}", orderId, reason);
    }

    public void publishStarted(UUID deliveryId, UUID orderId, List<DeliveryOrderItemEntity> items) {
        List<DeliveryOrderItemPayload> payloads = items.stream()
                .map(i -> DeliveryOrderItemPayload.builder()
                        .orderItemId(i.getOrderItemId())
                        .productId(i.getProductId())
                        .hubId(i.getHubId())
                        .quantity(i.getQuantity())
                        .build())
                .toList();

        DeliveryStartedEvent event = DeliveryStartedEvent.builder()
                .eventId(UUID.randomUUID())
                .deliveryId(deliveryId)
                .orderId(orderId)
                .orderItems(payloads)
                .build();

        outboxEventPublisher.publish(KafkaTopics.DELIVERY_STARTED, deliveryId.toString(), "DELIVERY", event);
        log.info("[Outbox] delivery.started 저장 deliveryId={}", deliveryId);
    }

    public void publishCancelledAck(UUID deliveryId, UUID orderId) {
        DeliveryCancelledAckEvent event = DeliveryCancelledAckEvent.builder()
                .eventId(UUID.randomUUID())
                .deliveryId(deliveryId)
                .orderId(orderId)
                .build();

        outboxEventPublisher.publish(KafkaTopics.DELIVERY_CANCELLED_ACK, orderId.toString(), "DELIVERY", event);
        log.info("[Outbox] delivery.cancelled.ack 저장 orderId={}", orderId);
    }

    public void publishCancellationFailed(UUID orderId, UUID deliveryId, String reason) {
        DeliveryCancellationFailedEvent event = DeliveryCancellationFailedEvent.builder()
                .eventId(UUID.randomUUID())
                .orderId(orderId)
                .deliveryId(deliveryId)
                .reason(reason)
                .build();

        outboxEventPublisher.publish(KafkaTopics.DELIVERY_CANCELLATION_FAILED, orderId.toString(), "DELIVERY", event);
        log.info("[Outbox] delivery.cancellation.failed 저장 orderId={} reason={}", orderId, reason);
    }
}
