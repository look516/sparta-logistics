package com.sparta.logistics.common.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sparta.logistics.common.security.GatewayAuthEntryPoint;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * GatewayAuthFilter X-User-Role 유효성 검증 테스트
 *
 * 수정 전: Role enum 검증 없어 임의 문자열 role로 인증 통과 가능
 * 수정 후: Role enum 외 값은 401 응답 반환
 */
@ExtendWith(MockitoExtension.class)
class GatewayAuthFilterRoleTest {

    @Mock FilterChain filterChain;

    GatewayAuthEntryPoint gatewayAuthEntryPoint;
    GatewayAuthFilter filter;

    @BeforeEach
    void setUp() {
        gatewayAuthEntryPoint = new GatewayAuthEntryPoint(new ObjectMapper());
        filter = new GatewayAuthFilter(gatewayAuthEntryPoint);
    }

    @Test
    @DisplayName("유효한 Role(MASTER) 헤더 - 필터 통과")
    void validRole_filterPasses() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-User-Id", UUID.randomUUID().toString());
        request.addHeader("X-User-Role", "MASTER");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(response.getStatus()).isNotEqualTo(401);
    }

    @Test
    @DisplayName("유효한 Role(HUB_MANAGER) 헤더 - 필터 통과")
    void validRoleHubManager_filterPasses() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-User-Id", UUID.randomUUID().toString());
        request.addHeader("X-User-Role", "HUB_MANAGER");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("존재하지 않는 Role 값 - 401 반환, filterChain 미호출")
    void invalidRole_returns401() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-User-Id", UUID.randomUUID().toString());
        request.addHeader("X-User-Role", "SUPER_ADMIN");  // 존재하지 않는 Role
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain, never()).doFilter(any(), any());
        assertThat(response.getStatus()).isEqualTo(401);
    }

    @Test
    @DisplayName("소문자 role 값 - 401 반환 (enum은 대문자만 허용)")
    void lowercaseRole_returns401() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-User-Id", UUID.randomUUID().toString());
        request.addHeader("X-User-Role", "master");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain, never()).doFilter(any(), any());
        assertThat(response.getStatus()).isEqualTo(401);
    }

    @Test
    @DisplayName("X-User-Id가 UUID 형식이 아닌 경우 - 401 반환")
    void invalidUserId_returns401() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-User-Id", "not-a-uuid");
        request.addHeader("X-User-Role", "MASTER");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain, never()).doFilter(any(), any());
        assertThat(response.getStatus()).isEqualTo(401);
    }

    @Test
    @DisplayName("X-User 헤더가 없는 경우 - filterChain 호출 (인증 없이 통과, Spring Security가 처리)")
    void noHeaders_filterChainCalled() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }
}
