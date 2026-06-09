package com.sparta.logistics.gateway.exception;

import org.springframework.http.HttpStatus;

/**
 * Gateway 전용 ErrorCode 인터페이스
 *
 * NOTE: common 모듈의 ErrorCode와 구조가 동일하지만 의도적으로 분리되어 있습니다.
 * api-gateway는 Spring WebFlux(Reactive) 기반으로, common 모듈(Spring WebMVC)을
 * 의존하면 서블릿/리액티브 충돌이 발생하므로 common을 참조할 수 없습니다.
 * 따라서 인터페이스 정의를 여기에 유지합니다.
 * (JwtErrorCode가 이 인터페이스를 구현합니다.)
 */
public interface ErrorCode {
    HttpStatus getStatus();
    String getCode();
    String getMessage();
}
