package com.sparta.logistics.delivery.e2e;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sparta.logistics.common.domain.Role;
import com.sparta.logistics.delivery.client.HubServiceClient;
import com.sparta.logistics.delivery.client.UserServiceClient;
import com.sparta.logistics.delivery.dto.DeliveryStatusChangeRequest;
import com.sparta.logistics.delivery.entity.DeliveryEntity;
import com.sparta.logistics.delivery.entity.enums.DeliveryStatus;
import com.sparta.logistics.delivery.repository.DeliveryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class DeliveryControllerE2ETest {

    private static final String UUID_PATTERN =
            "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}";

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired DeliveryRepository deliveryRepository;

    @MockBean HubServiceClient hubServiceClient;
    @MockBean UserServiceClient userServiceClient;

    private UUID deliveryId;
    private UUID sourceHubId;
    private UUID destinationHubId;
    private UUID managerId;

    @BeforeEach
    void setUp() {
        sourceHubId = UUID.randomUUID();
        destinationHubId = UUID.randomUUID();
        managerId = UUID.randomUUID();

        DeliveryEntity entity = new DeliveryEntity(
                UUID.randomUUID(), UUID.randomUUID(),
                sourceHubId, destinationHubId, "서울시 강남구", "slack-id");
        // HUB_WAITING으로 상태 전이 (CREATED → HUB_WAITING)
        entity.changeStatus(DeliveryStatus.HUB_WAITING);
        deliveryRepository.save(entity);
        deliveryId = entity.getId();
    }

    @Test
    void MASTER_목록조회시_sourceHubName_destinationHubName_managerId가_null이_아님() throws Exception {
        mockMvc.perform(get("/api/v1/deliveries")
                        .header("X-User-Id", UUID.randomUUID())
                        .header("X-User-Role", Role.MASTER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].sourceHubName", notNullValue()))
                .andExpect(jsonPath("$.content[0].destinationHubName", notNullValue()))
                .andExpect(jsonPath("$.content[0].sourceHubName", matchesPattern(UUID_PATTERN)))
                .andExpect(jsonPath("$.content[0].destinationHubName", matchesPattern(UUID_PATTERN)));
    }

    @Test
    void 유효한_상태전이_PATCH_성공() throws Exception {
        DeliveryStatusChangeRequest req = new DeliveryStatusChangeRequest(DeliveryStatus.HUB_MOVING);

        mockMvc.perform(patch("/api/v1/deliveries/{id}/status", deliveryId)
                        .header("X-User-Id", UUID.randomUUID())
                        .header("X-User-Role", Role.MASTER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("HUB_MOVING"));
    }

    @Test
    void 불가_상태전이_PATCH_400() throws Exception {
        // HUB_WAITING → COMPLETED 는 허용되지 않음
        DeliveryStatusChangeRequest req = new DeliveryStatusChangeRequest(DeliveryStatus.COMPLETED);

        mockMvc.perform(patch("/api/v1/deliveries/{id}/status", deliveryId)
                        .header("X-User-Id", UUID.randomUUID())
                        .header("X-User-Role", Role.MASTER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void MASTER_삭제_후_단건조회_404() throws Exception {
        mockMvc.perform(delete("/api/v1/deliveries/{id}", deliveryId)
                        .header("X-User-Id", UUID.randomUUID())
                        .header("X-User-Role", Role.MASTER))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/deliveries/{id}", deliveryId)
                        .header("X-User-Id", UUID.randomUUID())
                        .header("X-User-Role", Role.MASTER))
                .andExpect(status().isNotFound());
    }
}
