# delivery-service 부하 테스트

## 사전 준비

### k6 설치

**macOS**
```bash
brew install k6
```

**Windows (winget)**
```powershell
winget install k6 --source winget
```

**Docker**
```bash
docker pull grafana/k6
```

## 실행 방법

### 1. 로컬 서비스 기동
delivery-service(포트 19096)를 먼저 실행합니다.

### 2. 테스트 데이터 준비
DB에 조회/배차 가능한 배송 데이터(DELIVERY_ID)와 MASTER 권한 유저(USER_ID)가 있어야 합니다.

### 3. 스크립트 실행

```bash
# 기본 실행 (localhost:19096)
k6 run delivery-service/loadtest/delivery-load-test.js

# 환경변수 지정
k6 run \
  --env BASE_URL=http://localhost:19096 \
  --env USER_ID=<your-user-uuid> \
  --env DELIVERY_ID=<your-delivery-uuid> \
  delivery-service/loadtest/delivery-load-test.js

# Docker로 실행
docker run --rm -i --network host grafana/k6 run - \
  --env BASE_URL=http://host.docker.internal:19096 \
  < delivery-service/loadtest/delivery-load-test.js
```

## 시나리오

| 구간 | 시간 | VU |
|---|---|---|
| 워밍업 (ramp-up) | 30초 | 0 → 10 |
| 부하 유지 (steady) | 1분 | 50 |
| 종료 (ramp-down) | 30초 | 50 → 0 |

## 성공 기준 (Thresholds)

| 지표 | 기준 |
|---|---|
| `http_req_duration p(95)` | < 500ms |
| `error_rate` | < 1% |
| `list_latency p(95)` | < 500ms |
| `detail_latency p(95)` | < 300ms |
| `assign_latency p(95)` | < 800ms |

## 결과 저장

```bash
# JSON으로 저장
k6 run --out json=result.json delivery-load-test.js

# InfluxDB + Grafana 연동 (선택)
k6 run --out influxdb=http://localhost:8086/k6 delivery-load-test.js
```
