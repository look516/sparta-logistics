package com.sparta.logistics.delivery.e2e;

import com.sparta.logistics.delivery.client.HubServiceClient;
import com.sparta.logistics.delivery.client.UserServiceClient;
import com.sparta.logistics.delivery.entity.DeliveryEntity;
import com.sparta.logistics.delivery.entity.DeliveryManagerEntity;
import com.sparta.logistics.delivery.entity.DeliveryRouteEntity;
import com.sparta.logistics.delivery.entity.enums.DeliveryManagerStatus;
import com.sparta.logistics.delivery.entity.enums.DeliveryManagerType;
import com.sparta.logistics.delivery.entity.enums.DeliveryStatus;
import com.sparta.logistics.delivery.entity.enums.RouteStatus;
import com.sparta.logistics.delivery.entity.enums.RouteType;
import com.sparta.logistics.delivery.repository.DeliveryManagerRepository;
import com.sparta.logistics.delivery.repository.DeliveryRepository;
import com.sparta.logistics.delivery.repository.DeliveryRouteRepository;
import com.sparta.logistics.delivery.service.DeliveryRouteService;
import com.sparta.logistics.common.domain.Role;
import com.sparta.logistics.delivery.dto.route.DeliveryRouteUpdateRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

// 9fbb533 회귀 방지 — 다중 구간 HUB_TO_HUB IN_TRANSIT 상태 전이 조건
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class DeliveryStatusTransitionE2ETest {

    @Autowired DeliveryRepository deliveryRepository;
    @Autowired DeliveryRouteRepository routeRepository;
    @Autowired DeliveryManagerRepository managerRepository;
    @Autowired DeliveryRouteService deliveryRouteService;

    @MockBean HubServiceClient hubServiceClient;
    @MockBean UserServiceClient userServiceClient;

    @Test
    void 다중구간_첫_번째_구간_ARRIVED_후_배송상태는_HUB_MOVING_유지() {
        DeliveryEntity delivery = savedDelivery();
        DeliveryManagerEntity manager = savedManager(delivery.getSourceHubId());

        DeliveryRouteEntity route1 = savedRoute(delivery, 0, RouteType.HUB_TO_HUB, manager.getId());
        savedRoute(delivery, 1, RouteType.HUB_TO_HUB, null);
        savedRoute(delivery, 2, RouteType.HUB_TO_COMPANY, null);

        // WAITING → IN_TRANSIT → ARRIVED
        deliveryRouteService.updateRoute(delivery.getId(), route1.getId(),
                new DeliveryRouteUpdateRequest(RouteStatus.IN_TRANSIT, null, null),
                UUID.randomUUID(), Role.MASTER, null);
        deliveryRouteService.updateRoute(delivery.getId(), route1.getId(),
                new DeliveryRouteUpdateRequest(RouteStatus.ARRIVED, null, null),
                UUID.randomUUID(), Role.MASTER, null);

        DeliveryEntity updated = deliveryRepository.findById(delivery.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(DeliveryStatus.HUB_MOVING);
    }

    @Test
    void 다중구간_마지막_허브구간_ARRIVED_후_DESTINATION_HUB_ARRIVED_전이() {
        DeliveryEntity delivery = savedDelivery();
        DeliveryManagerEntity manager1 = savedManager(delivery.getSourceHubId());
        DeliveryManagerEntity manager2 = savedManager(delivery.getSourceHubId());

        DeliveryRouteEntity route1 = savedRoute(delivery, 0, RouteType.HUB_TO_HUB, manager1.getId());
        DeliveryRouteEntity route2 = savedRoute(delivery, 1, RouteType.HUB_TO_HUB, manager2.getId());
        savedRoute(delivery, 2, RouteType.HUB_TO_COMPANY, null);

        // 1구간 완료
        deliveryRouteService.updateRoute(delivery.getId(), route1.getId(),
                new DeliveryRouteUpdateRequest(RouteStatus.IN_TRANSIT, null, null),
                UUID.randomUUID(), Role.MASTER, null);
        deliveryRouteService.updateRoute(delivery.getId(), route1.getId(),
                new DeliveryRouteUpdateRequest(RouteStatus.ARRIVED, null, null),
                UUID.randomUUID(), Role.MASTER, null);

        // 2구간(마지막 HUB_TO_HUB) 완료 → 다음이 HUB_TO_COMPANY(라스트마일)
        deliveryRouteService.updateRoute(delivery.getId(), route2.getId(),
                new DeliveryRouteUpdateRequest(RouteStatus.IN_TRANSIT, null, null),
                UUID.randomUUID(), Role.MASTER, null);
        deliveryRouteService.updateRoute(delivery.getId(), route2.getId(),
                new DeliveryRouteUpdateRequest(RouteStatus.ARRIVED, null, null),
                UUID.randomUUID(), Role.MASTER, null);

        DeliveryEntity updated = deliveryRepository.findById(delivery.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(DeliveryStatus.DESTINATION_HUB_ARRIVED);
    }

    private DeliveryEntity savedDelivery() {
        DeliveryEntity e = new DeliveryEntity(
                UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), UUID.randomUUID(), "주소", "slack");
        e.changeStatus(DeliveryStatus.HUB_WAITING);
        e.changeStatus(DeliveryStatus.HUB_MOVING);
        return deliveryRepository.save(e);
    }

    private DeliveryManagerEntity savedManager(UUID hubId) {
        DeliveryManagerEntity m = new DeliveryManagerEntity(
                UUID.randomUUID(), hubId, "slack-test", DeliveryManagerType.HUB_DELIVERY, 0);
        return managerRepository.save(m);
    }

    private DeliveryRouteEntity savedRoute(DeliveryEntity delivery, int seq,
                                            RouteType type, UUID managerId) {
        DeliveryRouteEntity r = new DeliveryRouteEntity(
                delivery, seq, type,
                UUID.randomUUID(), UUID.randomUUID(),
                BigDecimal.valueOf(100), 60);
        if (managerId != null) r.assignManager(managerId);
        return routeRepository.save(r);
    }
}
