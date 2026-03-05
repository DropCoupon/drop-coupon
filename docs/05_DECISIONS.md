# Decisions & Policies (Spring Boot)
대용량 트래픽 대응 선착순 쿠폰 발급 시스템 — 구현 정책 확정본 v1.0

## 1) Queue
- Kafka 사용 (로컬: Redpanda)
- Topic: `coupon-claims`
- 메시지 키(key): `requestId` (파티션 안정성 + 순서 힌트)

---

## 2) 클라이언트 결과 조회 방식
### 기본: SSE 권장
- Claim API는 즉시 `202 ACCEPTED + requestId` 반환
- 클라이언트는 `GET /claims/stream?requestId=...` 로 SSE 연결을 열고 상태 이벤트를 받는다.
- 최종 상태(ISSUED/DUPLICATE/SOLD_OUT/FAILED)에 도달하면 서버가 `event: done`을 보내고 연결 종료를 권장한다.

### SSE가 롱폴링보다 유리한 이유(실전 관점)
- 롱폴링은 “요청-응답” 왕복이 반복되어 **HTTP 요청 오버헤드**가 누적됨
- SSE는 한 번 연결 후 이벤트를 push 하므로 **왕복 비용/서버 라우팅 비용 감소**
- “요청 폭주 시간대”에 폴링은 서버에 불필요한 read 트래픽을 추가할 위험이 큼

### SSE 도입 시 주의점(중요)
- **프록시/로드밸런서 타임아웃**: idle timeout(예: 60s~)에 끊길 수 있어 heartbeat 필요
- **연결 수 관리**: SSE는 연결을 유지하므로 동시 사용자 수가 크면 커넥션 수가 늘어난다
  - 대응: 최종 상태 도달 시 즉시 종료, heartbeat 주기(예: 15s), 최대 연결 시간 제한(예: 2~5분)
- **모바일/불안정 네트워크**에서 끊김이 잦을 수 있음 → 자동 재연결 + fallback 필요

### Fallback: 폴링 엔드포인트 유지(권장)
- SSE가 막히는 환경/클라이언트가 있으므로 `GET /claims/{requestId}` 폴링도 유지한다.
- 다만 클라이언트 기본 구현은 SSE 우선, 폴링은 fallback으로 둔다.

---

## 3) requestId 정책(클라이언트 강제) + TTL 10분
### 정책
- 클라이언트는 `requestId`를 반드시 생성해서 `POST /campaigns/{id}/claim`에 전달한다.
- 서버는 requestId를 기준으로 멱등성을 보장한다.
- Redis 보조 키(옵션): `idempotency:{requestId}` TTL=10분

### TTL 10분의 “위험성/트레이드오프” (요청한 부분)
TTL 10분은 흔히 쓰는 값이지만 아래 위험이 있다:

1) **처리 지연이 10분을 넘으면 멱등성 보조키가 만료**
- Kafka 적체/DB 장애로 확정이 늦어지면, 10분 이후 동일 requestId 재시도에서
  “Redis 보조키로는” 중복 차단이 안 될 수 있다.
- 단, 우리는 **DB의 `unique(request_id)`**가 최종 멱등성을 보장하므로 “정합성 붕괴”로 이어지진 않는다.
- 하지만 Redis 보조키 만료 이후에는 API가 DB 조회/처리를 더 하게 되어 **부하가 늘 수 있음**.

2) **사용자가 10분 뒤 재시도 시 UX 혼란 가능**
- 클라이언트가 같은 requestId를 유지하며 재시도하면 결과는 동일하게 나오지만,
  클라이언트 구현이 requestId를 다시 생성해버리면 “새 요청”으로 인식되어
  SOLD_OUT/중복 등의 응답이 더 자주 발생할 수 있다.
- 따라서 클라이언트는 “요청 시작 ~ 최종 결과 확정까지” 동일 requestId를 유지해야 한다.

3) **메모리/키 폭증**
- TTL이 길수록 Redis 키가 오래 남아 메모리를 더 먹는다.
- 10분은 비교적 현실적인 타협이지만, 피크 트래픽이 크면 키 수가 급격히 늘 수 있다.
- 대응: 키에 최소 정보만 저장, 압축된 값, TTL 엄수, rate limit 병행.

### 권장 안전장치
- “보조키”는 어디까지나 최적화/빠른 차단용으로만 사용하고,
  **최종 멱등성은 DB unique(request_id)로 보장**한다.
- 처리 지연이 길어질 때를 대비해 보호모드로 claim을 제한(5번 정책)한다.

---

## 4) 재고 초기화/리셋 정책 (권장 채택)
- `PATCH /admin/campaigns/{id}/activate` 시:
  - `stock:{id}`가 이미 존재하면 **overwrite 금지** (운영 실수 방지)
  - 존재하면 409(CONFLICT) 또는 명시적 에러 코드 반환
- 재고를 다시 세팅해야 하면 별도 API로 분리(옵션):
  - `POST /admin/campaigns/{id}/reset-stock` (관리자만, audit 로그 필수)

---

## 5) Redis 장애 정책
- Redis 장애/타임아웃 시 claim 기능은 **503으로 fail-safe**
- “정합성 우선 + 보호모드” 채택:
  - Redis/DB/Kafka 상태가 불안정하면 과감히 제한하여
    초과발급/데이터 꼬임 가능성을 차단

---

## 6) 보상(재고 INCR) 규칙 (권장)
Redis 선차감 성공 후 DB 확정 과정에서 아래 규칙을 적용한다.

### Worker 처리 결과별 규칙
- DB INSERT 성공(ISSUED):
  - 보상 없음
- DB unique(campaign_id,user_id) 위반(DUPLICATE):
  - **재고 INCR 보상 1회**
  - 상태를 DUPLICATE로 기록
- DB unique(request_id) 위반(중복 메시지/재처리):
  - **보상 없음** (이미 같은 요청이 처리된 것)
  - 기존 레코드 조회 후 상태를 그대로 유지
- 기타 예외(DB 일시 장애 등):
  - 재시도(backoff)
  - 재시도 한도 초과 시 FAILED로 마킹 + **재고 INCR 보상 1회**
  - (옵션) DLQ로 이동

### “보상 중복” 방지
- 보상은 반드시 “이 요청이 재고를 예약했는데 확정 실패/중복으로 귀결된 경우”에만 1회 수행
- 중복 메시지로 같은 requestId가 다시 처리될 때는 보상이 중복 적용되지 않도록
  requestId 상태를 기반으로 판단한다.

---

## 7) 테스트 전략 (권장 채택)
- 통합 테스트: Testcontainers로 통일(Postgres/Redis/Kafka 가능하면 포함)
- 동시성 테스트:
  - JUnit 멀티스레드(소규모): stock=100, 요청=200 같은 검증
  - k6(대규모): spike/soak/oversubscription
- 목표:
  - 초과발급 0, 중복발급 0을 “테스트로 증명”

---

## 8) 인증/사용자 식별 (권장 채택)
- MVP: `X-User-Id` 헤더 기반 (간단/명확/테스트 쉬움)
- (옵션) 후속 PR에서 JWT 도입 가능하나, 핵심 목표(대용량 트래픽/정합성)와는 분리한다.