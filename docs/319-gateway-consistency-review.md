# [#319] api-gateway / common 모듈 정합성 검토 결과

## 검토 배경
api-gateway와 common 모듈 간 `ErrorCode`, `ApiResponse` 중복 정의 문제가 있어
통합 가능 여부를 검토함.

---

## 결론: **현행 분리 구조 유지** (코드 변경 없음)

---

## 이유

### api-gateway는 WebFlux, 나머지 서비스는 WebMVC
| 모듈 | 기반 |
|---|---|
| `api-gateway` | Spring WebFlux (Reactive) |
| `common`, 각 도메인 서비스 | Spring WebMVC (Servlet) |

`api-gateway`가 `common` 모듈을 의존할 경우 아래 충돌이 발생함:
- `DispatcherServlet` (WebMVC) vs `DispatcherHandler` (WebFlux) 충돌
- `spring-security-web` (Servlet) vs `spring-security-webflux` 충돌
- `HttpServletRequest` / `HttpServletResponse` 사용 불가

→ **api-gateway의 `ErrorCode`, `ApiResponse` 중복 정의는 의도된 설계임**

---

## 검토한 항목별 판단

### 1. api-gateway `ErrorCode` 통합
- **불가**: common 의존 불가로 인해 분리 유지
- `api-gateway/exception/ErrorCode.java` (인터페이스)와 `common/exception/ErrorCode.java` (인터페이스)는 동일한 메서드 시그니처(`getStatus()`, `getCode()`, `getMessage()`)를 각자 유지

### 2. api-gateway `ApiResponse` 동기화
- **불가**: 동일 이유로 분리 유지
- 단, `error()` 메서드의 상태코드 하드코딩 버그는 **#316에서 common쪽만 수정 완료**
- api-gateway `ApiResponse.error()` 동일 버그 존재 → **api-gateway 담당자가 별도 수정 필요**

### 3. `GatewayAuthFilter` X-User-Role 헤더 검증 보강
- **보류**: `GatewayAuthFilter`는 common 모듈에 위치하여 전 서비스에 영향
- Role enum 검증 로직 추가 시 팀 전체 협의 필요
- 현행 코드에서 유효하지 않은 Role이 들어와도 `ROLE_<임의값>` 권한이 부여되는 보안 취약점 존재
  → 추후 팀 논의 후 적용 권장

---

## api-gateway 담당자에게 전달 필요한 사항

```java
// api-gateway/response/ApiResponse.java
// 현재 (버그): error() 메서드가 status를 항상 400으로 하드코딩
public static <T> ApiResponse<T> error(ErrorCode errorCode) {
    return new ApiResponse<>(false, 400, errorCode.getMessage(), null);  // ← 버그
}

// 수정 필요:
public static <T> ApiResponse<T> error(ErrorCode errorCode) {
    return new ApiResponse<>(false, errorCode.getStatus().value(), errorCode.getMessage(), null);
}
public static <T> ApiResponse<T> error(ErrorCode errorCode, String message) {
    return new ApiResponse<>(false, errorCode.getStatus().value(), message, null);
}
```
