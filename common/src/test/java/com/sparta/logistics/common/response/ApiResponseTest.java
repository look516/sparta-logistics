package com.sparta.logistics.common.response;

import com.sparta.logistics.common.exception.CommonErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ApiResponseTest {

    @Test
    @DisplayName("error() - ErrorCode의 상태코드가 바디 status에 반영된다")
    void error_statusMatchesErrorCode() {
        // UNAUTHORIZED = 401
        ApiResponse<Void> response = ApiResponse.error(CommonErrorCode.UNAUTHORIZED);
        assertThat(response.success()).isFalse();
        assertThat(response.status()).isEqualTo(401);
        assertThat(response.message()).isEqualTo(CommonErrorCode.UNAUTHORIZED.getMessage());
    }

    @Test
    @DisplayName("error() - FORBIDDEN(403)이 400이 아닌 403을 반환한다")
    void error_forbiddenReturns403() {
        ApiResponse<Void> response = ApiResponse.error(CommonErrorCode.FORBIDDEN);
        assertThat(response.status()).isEqualTo(403);
    }

    @Test
    @DisplayName("error() - INTERNAL_SERVER_ERROR(500)이 올바르게 반환된다")
    void error_internalServerErrorReturns500() {
        ApiResponse<Void> response = ApiResponse.error(CommonErrorCode.INTERNAL_SERVER_ERROR);
        assertThat(response.status()).isEqualTo(500);
    }

    @Test
    @DisplayName("error(errorCode, message) - 커스텀 메시지와 ErrorCode 상태코드가 반영된다")
    void errorWithMessage_statusAndCustomMessageReflected() {
        String customMessage = "상세 오류 메시지";
        ApiResponse<Void> response = ApiResponse.error(CommonErrorCode.VALIDATION_FAILED, customMessage);
        assertThat(response.success()).isFalse();
        assertThat(response.status()).isEqualTo(400);
        assertThat(response.message()).isEqualTo(customMessage);
    }

    @Test
    @DisplayName("ok() - success=true, status=200")
    void ok_successTrue() {
        ApiResponse<String> response = ApiResponse.ok("data");
        assertThat(response.success()).isTrue();
        assertThat(response.status()).isEqualTo(200);
        assertThat(response.data()).isEqualTo("data");
    }
}
