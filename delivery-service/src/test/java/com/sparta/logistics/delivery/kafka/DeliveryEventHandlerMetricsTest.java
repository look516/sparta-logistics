package com.sparta.logistics.delivery.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sparta.logistics.common.kafka.event.CancelDeliveryCommand;
import com.sparta.logistics.common.response.ApiResponse;
import com.sparta.logistics.delivery.client.FeignCallService;
import com.sparta.logistics.delivery.client.response.HubRouteSegmentResponse;
import com.sparta.logistics.delivery.client.response.UserResponse;
import com.sparta.logistics.delivery.dto.event.StockReservedEventDto;
import com.sparta.logistics.delivery.dto.event.StockReservedItemPayload;
import com.sparta.logistics.delivery.kafka.consumer.DeliveryEventHandler;
import com.sparta.logistics.delivery.kafka.producer.DeliveryEventPublisher;
import com.sparta.logistics.delivery.repository.DeliveryRepository;
import com.sparta.logistics.delivery.service.DeliveryManagerService;
import com.sparta.logistics.delivery.service.DeliveryService;
import io.micrometer.core.instrument.Counter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * DeliveryEventHandler 메트릭 카운터 증가 검증
 * - 배송 생성 성공/실패 시 해당 카운터가 increment() 되는지 확인
 * - 배송 취소 성공/실패 시 해당 카운터가 increment() 되는지 확인
 */
@ExtendWith(MockitoExtension.class)
class DeliveryEventHandlerMetricsTest {

    @Mock DeliveryService deliveryService;
    @Mock DeliveryManagerService deliveryManagerService;
    @Mock DeliveryEventPublisher eventPublisher;
    @Mock FeignCallService feignCallService;
    @Mock DeliveryRepository deliveryRepository;
    @Mock Counter deliveryCreationSuccessCounter;
    @Mock Counter deliveryCreationFailureCounter;
    @Mock Counter deliveryCancelSuccessCounter;
    @Mock Counter deliveryCancelFailureCounter;

    DeliveryEventHandler handler;
    ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
        handler = new DeliveryEventHandler(
                deliveryService, deliveryManagerService, eventPublisher,
                feignCallService, objectMapper, deliveryRepository,
                deliveryCreationSuccessCounter, deliveryCreationFailureCounter,
                deliveryCancelSuccessCounter, deliveryCancelFailureCounter
        );
    }

    @Test
    @DisplayName("배송 생성 성공 시 creationSuccessCounter.increment() 호출")
    void handleStockReserved_success_incrementsSuccessCounter() throws Exception {
        UUID orderId = UUID.randomUUID();
        UUID receiverId = UUID.randomUUID();
        UUID sourceHubId = UUID.randomUUID();
        UUID destHubId = UUID.randomUUID();

        when(feignCallService.fetchUser(receiverId))
                .thenReturn(ApiResponse.ok(new UserResponse(receiverId, "slack-42")));
        when(feignCallService.fetchRouteSegments(sourceHubId, destHubId))
                .thenReturn(List.of(new HubRouteSegmentResponse(1, true, sourceHubId, destHubId, BigDecimal.valueOf(100), 60)));

        StockReservedEventDto event = new StockReservedEventDto(
                orderId, receiverId, sourceHubId, destHubId,
                "주소", "서울허브", "부산허브",
                List.of(new StockReservedItemPayload(UUID.randomUUID(), UUID.randomUUID(), sourceHubId, 1)),
                1
        );

        handler.handleStockReserved(objectMapper.writeValueAsString(event), "topic", 0, 0L);

        verify(deliveryCreationSuccessCounter).increment();
    }

    @Test
    @DisplayName("배송 생성 실패(createDelivery 예외) 시 creationFailureCounter.increment() 호출")
    void handleStockReserved_createFails_incrementsFailureCounter() throws Exception {
        UUID orderId = UUID.randomUUID();
        UUID receiverId = UUID.randomUUID();
        UUID sourceHubId = UUID.randomUUID();
        UUID destHubId = UUID.randomUUID();

        when(feignCallService.fetchUser(receiverId))
                .thenReturn(ApiResponse.ok(new UserResponse(receiverId, "slack-42")));
        when(feignCallService.fetchRouteSegments(sourceHubId, destHubId))
                .thenReturn(List.of(new HubRouteSegmentResponse(1, true, sourceHubId, destHubId, BigDecimal.valueOf(100), 60)));
        doThrow(new RuntimeException("DB error")).when(deliveryService).createDelivery(any(), any(), any());

        StockReservedEventDto event = new StockReservedEventDto(
                orderId, receiverId, sourceHubId, destHubId,
                "주소", "서울허브", "부산허브",
                List.of(new StockReservedItemPayload(UUID.randomUUID(), UUID.randomUUID(), sourceHubId, 1)),
                1
        );

        handler.handleStockReserved(objectMapper.writeValueAsString(event), "topic", 0, 0L);

        verify(deliveryCreationFailureCounter).increment();
    }

    @Test
    @DisplayName("배송 취소 성공(cancelled=true) 시 cancelSuccessCounter.increment() 호출")
    void handleCancelCommand_success_incrementsCancelSuccessCounter() throws Exception {
        UUID deliveryId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();

        when(deliveryService.cancelDeliveryByCommand(deliveryId)).thenReturn(true);

        CancelDeliveryCommand command = CancelDeliveryCommand.builder()
                .eventId(UUID.randomUUID())
                .deliveryId(deliveryId)
                .orderId(orderId)
                .build();

        handler.handleCancelDeliveryCommand(objectMapper.writeValueAsString(command));

        verify(deliveryCancelSuccessCounter).increment();
    }

    @Test
    @DisplayName("배송 취소 불가(cancelled=false) 시 cancelFailureCounter.increment() 호출")
    void handleCancelCommand_inTransit_incrementsCancelFailureCounter() throws Exception {
        UUID deliveryId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();

        when(deliveryService.cancelDeliveryByCommand(deliveryId)).thenReturn(false);

        CancelDeliveryCommand command = CancelDeliveryCommand.builder()
                .eventId(UUID.randomUUID())
                .deliveryId(deliveryId)
                .orderId(orderId)
                .build();

        handler.handleCancelDeliveryCommand(objectMapper.writeValueAsString(command));

        verify(deliveryCancelFailureCounter).increment();
    }
}
