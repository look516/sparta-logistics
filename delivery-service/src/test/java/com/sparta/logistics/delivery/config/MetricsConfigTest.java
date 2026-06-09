package com.sparta.logistics.delivery.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * MetricsConfig 카운터 빈 등록 및 증가 검증
 */
class MetricsConfigTest {

    SimpleMeterRegistry registry;
    MetricsConfig config;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        config   = new MetricsConfig();
    }

    @Test
    @DisplayName("deliveryCreationSuccessCounter - 카운터가 등록되고 증가한다")
    void creationSuccess_counterIncrements() {
        Counter counter = config.deliveryCreationSuccessCounter(registry);
        counter.increment();
        counter.increment();
        assertThat(registry.counter("delivery.kafka.stock_reserved.success").count()).isEqualTo(2.0);
    }

    @Test
    @DisplayName("deliveryCreationFailureCounter - 카운터가 등록되고 증가한다")
    void creationFailure_counterIncrements() {
        Counter counter = config.deliveryCreationFailureCounter(registry);
        counter.increment();
        assertThat(registry.counter("delivery.kafka.stock_reserved.failure").count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("deliveryCancelSuccessCounter - 카운터가 등록되고 증가한다")
    void cancelSuccess_counterIncrements() {
        Counter counter = config.deliveryCancelSuccessCounter(registry);
        counter.increment();
        assertThat(registry.counter("delivery.kafka.cancel_command.success").count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("deliveryCancelFailureCounter - 카운터가 등록되고 증가한다")
    void cancelFailure_counterIncrements() {
        Counter counter = config.deliveryCancelFailureCounter(registry);
        counter.increment();
        assertThat(registry.counter("delivery.kafka.cancel_command.failure").count()).isEqualTo(1.0);
    }
}
