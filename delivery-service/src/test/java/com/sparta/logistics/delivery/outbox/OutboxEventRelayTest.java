package com.sparta.logistics.delivery.outbox;

import com.sparta.logistics.common.outbox.OutboxEvent;
import com.sparta.logistics.common.outbox.OutboxEventRepository;
import com.sparta.logistics.common.outbox.OutboxEventStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * OutboxEventRelay 단위 테스트
 *
 * 검증 항목:
 * 1. PENDING 이벤트 Kafka 발행 성공 → markSent() → status = SENT, save() 호출
 * 2. Kafka 발행 실패 → incrementRetry() → retryCount 증가, save() 호출
 * 3. incrementRetry() MAX_RETRY(3) 도달 → status = FAILED
 * 4. shutdown() 후 relay() → repository 조회 없음
 * 5. PENDING 이벤트 없음 → Kafka send 없음
 */
@ExtendWith(MockitoExtension.class)
class OutboxEventRelayTest {

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    private OutboxEventRelay relay;

    @BeforeEach
    void setUp() {
        relay = new OutboxEventRelay(outboxEventRepository, kafkaTemplate);
    }

    /** 테스트용 PENDING OutboxEvent 생성 */
    private OutboxEvent pendingEvent() {
        return OutboxEvent.of("test-topic", "agg-id", "DELIVERY", "{\"key\":\"val\"}");
    }

    @SuppressWarnings("unchecked")
    private CompletableFuture<SendResult<String, String>> successFuture() {
        return CompletableFuture.completedFuture(mock(SendResult.class));
    }

    // -----------------------------------------------------------------------
    // 1. 발행 성공
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("PENDING 이벤트 발행 성공 시 status=SENT 및 save() 호출")
    void relay_success_marksSentAndSaves() {
        OutboxEvent event = pendingEvent();
        when(outboxEventRepository.findTop100ByStatusOrderByCreatedAtAsc(OutboxEventStatus.PENDING))
                .thenReturn(List.of(event));
        when(kafkaTemplate.send(anyString(), anyString(), anyString()))
                .thenReturn(successFuture());

        relay.relay();

        assertThat(event.getStatus()).isEqualTo(OutboxEventStatus.SENT);
        verify(outboxEventRepository).save(event);
    }

    // -----------------------------------------------------------------------
    // 2. 발행 실패 → retryCount 증가
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Kafka 발행 실패 시 retryCount 1 증가 및 save() 호출")
    void relay_kafkaFails_incrementsRetryAndSaves() {
        OutboxEvent event = pendingEvent();
        when(outboxEventRepository.findTop100ByStatusOrderByCreatedAtAsc(OutboxEventStatus.PENDING))
                .thenReturn(List.of(event));
        when(kafkaTemplate.send(anyString(), anyString(), anyString()))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("Kafka unavailable")));

        relay.relay();

        assertThat(event.getRetryCount()).isEqualTo(1);
        assertThat(event.getStatus()).isEqualTo(OutboxEventStatus.PENDING);
        verify(outboxEventRepository).save(event);
    }

    // -----------------------------------------------------------------------
    // 3. MAX_RETRY 초과 → FAILED
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("MAX_RETRY(3회) 실패 시 status=FAILED 전이")
    void relay_maxRetryExceeded_statusFailed() {
        OutboxEvent event = pendingEvent();
        when(outboxEventRepository.findTop100ByStatusOrderByCreatedAtAsc(OutboxEventStatus.PENDING))
                .thenReturn(List.of(event));
        when(kafkaTemplate.send(anyString(), anyString(), anyString()))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("Kafka unavailable")));

        // 3회 릴레이 실패
        relay.relay();
        relay.relay();
        relay.relay();

        assertThat(event.getRetryCount()).isEqualTo(3);
        assertThat(event.getStatus()).isEqualTo(OutboxEventStatus.FAILED);
    }

    // -----------------------------------------------------------------------
    // 4. shutdown 후 relay → 아무 작업 없음
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("shutdown() 후 relay() 실행 시 repository 조회하지 않음")
    void relay_afterShutdown_noAction() {
        relay.shutdown();
        relay.relay();

        verify(outboxEventRepository, never()).findTop100ByStatusOrderByCreatedAtAsc(any());
        verify(kafkaTemplate, never()).send(anyString(), anyString(), anyString());
    }

    // -----------------------------------------------------------------------
    // 5. PENDING 이벤트 없음 → Kafka send 없음
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("PENDING 이벤트 없을 때 Kafka 발행하지 않음")
    void relay_noPendingEvents_noKafkaSend() {
        when(outboxEventRepository.findTop100ByStatusOrderByCreatedAtAsc(OutboxEventStatus.PENDING))
                .thenReturn(List.of());

        relay.relay();

        verify(kafkaTemplate, never()).send(anyString(), anyString(), anyString());
    }
}
