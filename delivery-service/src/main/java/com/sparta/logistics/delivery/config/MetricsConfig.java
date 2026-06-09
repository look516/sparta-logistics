package com.sparta.logistics.delivery.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * delivery-service 커스텀 비즈니스 메트릭 등록
 *
 * 수집 지표:
 *   - delivery.kafka.stock_reserved.success  : 배송 생성 성공 횟수
 *   - delivery.kafka.stock_reserved.failure  : 배송 생성 실패 횟수 (보상 이벤트 발행)
 *   - delivery.kafka.cancel_command.success  : 배송 취소 성공 횟수
 *   - delivery.kafka.cancel_command.failure  : 배송 취소 실패 횟수
 *
 * Prometheus 수집 경로: GET /actuator/prometheus
 */
@Configuration
public class MetricsConfig {

    @Bean
    public Counter deliveryCreationSuccessCounter(MeterRegistry registry) {
        return Counter.builder("delivery.kafka.stock_reserved.success")
                .description("배송 생성 성공 횟수")
                .register(registry);
    }

    @Bean
    public Counter deliveryCreationFailureCounter(MeterRegistry registry) {
        return Counter.builder("delivery.kafka.stock_reserved.failure")
                .description("배송 생성 실패 횟수 (보상 이벤트 발행)")
                .register(registry);
    }

    @Bean
    public Counter deliveryCancelSuccessCounter(MeterRegistry registry) {
        return Counter.builder("delivery.kafka.cancel_command.success")
                .description("배송 취소 성공 횟수")
                .register(registry);
    }

    @Bean
    public Counter deliveryCancelFailureCounter(MeterRegistry registry) {
        return Counter.builder("delivery.kafka.cancel_command.failure")
                .description("배송 취소 실패 횟수")
                .register(registry);
    }
}
