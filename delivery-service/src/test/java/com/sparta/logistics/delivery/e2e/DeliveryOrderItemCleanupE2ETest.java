package com.sparta.logistics.delivery.e2e;

import com.sparta.logistics.delivery.client.HubServiceClient;
import com.sparta.logistics.delivery.client.UserServiceClient;
import com.sparta.logistics.delivery.entity.DeliveryEntity;
import com.sparta.logistics.delivery.entity.DeliveryOrderItemEntity;
import com.sparta.logistics.delivery.repository.DeliveryOrderItemRepository;
import com.sparta.logistics.delivery.repository.DeliveryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

// 1931ed3 회귀 방지 — deleteByDelivery_Id 실제 DB 삭제 동작 검증
// afterCommit 내 호출 시나리오는 DeliveryServiceOrderItemCleanupTest(단위) 에서 커버
@SpringBootTest
@ActiveProfiles("test")
class DeliveryOrderItemCleanupE2ETest {

    @Autowired DeliveryRepository deliveryRepository;
    @Autowired DeliveryOrderItemRepository orderItemRepository;

    @MockBean HubServiceClient hubServiceClient;
    @MockBean UserServiceClient userServiceClient;

    @Test
    void deleteByDelivery_Id_호출_시_해당_배송의_OrderItem_전체_삭제() {
        DeliveryEntity delivery = new DeliveryEntity(
                UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), UUID.randomUUID(), "주소", "slack");
        deliveryRepository.save(delivery);

        orderItemRepository.save(new DeliveryOrderItemEntity(
                delivery, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), 1));
        orderItemRepository.save(new DeliveryOrderItemEntity(
                delivery, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), 2));
        assertThat(orderItemRepository.findByDelivery_Id(delivery.getId())).hasSize(2);

        orderItemRepository.deleteByDelivery_Id(delivery.getId());

        assertThat(orderItemRepository.findByDelivery_Id(delivery.getId())).isEmpty();
    }

    @Test
    void deleteByDelivery_Id_다른_배송_OrderItem_영향_없음() {
        DeliveryEntity delivery1 = deliveryRepository.save(new DeliveryEntity(
                UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), UUID.randomUUID(), "주소1", "slack1"));
        DeliveryEntity delivery2 = deliveryRepository.save(new DeliveryEntity(
                UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), UUID.randomUUID(), "주소2", "slack2"));

        orderItemRepository.save(new DeliveryOrderItemEntity(
                delivery1, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), 1));
        orderItemRepository.save(new DeliveryOrderItemEntity(
                delivery2, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), 1));

        orderItemRepository.deleteByDelivery_Id(delivery1.getId());

        assertThat(orderItemRepository.findByDelivery_Id(delivery1.getId())).isEmpty();
        assertThat(orderItemRepository.findByDelivery_Id(delivery2.getId())).hasSize(1);
    }
}
