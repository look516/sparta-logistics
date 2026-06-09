package com.sparta.logistics.delivery.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sparta.logistics.common.exception.BusinessException;
import com.sparta.logistics.common.kafka.event.HubDeletedEvent;
import com.sparta.logistics.delivery.client.FeignCallService;
import com.sparta.logistics.delivery.kafka.consumer.DeliveryEventHandler;
import com.sparta.logistics.delivery.kafka.producer.DeliveryEventPublisher;
import com.sparta.logistics.delivery.repository.DeliveryRepository;
import com.sparta.logistics.delivery.service.DeliveryManagerService;
import com.sparta.logistics.delivery.service.DeliveryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

/**
 * DeliveryEventHandler.handleHubDeleted() 예외 미전파 테스트
 *
 * 수정 전: 처리 실패 시 RuntimeException을 re-throw → Kafka 무한 재시도
 * 수정 후: 예외를 catch하고 로그만 남김 → 메시지 스킵(offset commit)
 */
@ExtendWith(MockitoExtension.class)
class DeliveryEventHandlerHubDeletedTest {

    @Mock DeliveryService deliveryService;
    @Mock DeliveryManagerService deliveryManagerService;
    @Mock DeliveryEventPublisher eventPublisher;
    @Mock FeignCallService feignCallService;
    @Mock DeliveryRepository deliveryRepository;

    DeliveryEventHandler handler;
    ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        handler = new DeliveryEventHandler(
                deliveryService, deliveryManagerService, eventPublisher,
                feignCallService, objectMapper, deliveryRepository
        );
    }

    @Test
    @DisplayName("handleHubDeleted - 처리 성공 시 예외 없음")
    void handleHubDeleted_success_noException() throws Exception {
        HubDeletedEvent event = HubDeletedEvent.builder()
                .hubId(UUID.randomUUID())
                .deletedBy(UUID.randomUUID())
                .build();

        String message = objectMapper.writeValueAsString(event);

        assertThatCode(() -> handler.handleHubDeleted(message)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("handleHubDeleted - RuntimeException 발생 시 예외가 밖으로 전파되지 않는다 (Kafka 재시도 방지)")
    void handleHubDeleted_runtimeException_doesNotPropagate() throws Exception {
        doThrow(new RuntimeException("DB error")).when(deliveryManagerService)
                .softDeleteManagersByHubId(any(), any());

        HubDeletedEvent event = HubDeletedEvent.builder()
                .hubId(UUID.randomUUID())
                .deletedBy(UUID.randomUUID())
                .build();

        String message = objectMapper.writeValueAsString(event);

        // 예외가 밖으로 나오면 안 됨 (Kafka가 재시도하게 되므로)
        assertThatCode(() -> handler.handleHubDeleted(message)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("handleHubDeleted - BusinessException 발생 시 예외가 밖으로 전파되지 않는다")
    void handleHubDeleted_businessException_doesNotPropagate() throws Exception {
        doThrow(new BusinessException(
                com.sparta.logistics.delivery.exception.DeliveryErrorCode.DELIVERY_NOT_FOUND))
                .when(deliveryManagerService).softDeleteManagersByHubId(any(), any());

        HubDeletedEvent event = HubDeletedEvent.builder()
                .hubId(UUID.randomUUID())
                .deletedBy(UUID.randomUUID())
                .build();

        String message = objectMapper.writeValueAsString(event);

        assertThatCode(() -> handler.handleHubDeleted(message)).doesNotThrowAnyException();
    }
}
