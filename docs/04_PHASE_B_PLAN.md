# Phase B Execution Plan (Spring Boot v3) — PR-sized + Concurrency Checklist
High-Concurrency Coupon Issuance System  
(대용량 트래픽 대응 선착순 쿠폰 발급 시스템)

---

## 0. Fixed Tech Stack
- Java 25
- Spring Boot 3.5+
- Gradle
- PostgreSQL
- Redis
- Kafka (Local: Redpanda)
- Flyway
- Testcontainers
- Micrometer + Prometheus (+ Grafana)
- k6

---

## 1. Fixed Package Structure
> 아래 구조는 끝까지 유지한다.
```
io.dropcoupon
├── common
│    ├── config
│    ├── exception
│    ├── util
│    └── web
├── campaign
│    ├── controller
│    ├── service
│    ├── domain
│    └── repository
├── claim
│    ├── controller
│    ├── service
│    ├── domain
│    ├── repository
│    └── event
└── infrastructure
├── persistence
├── redis
└── kafka
```

---

## 2. Concurrency Model (핵심 동시성 설계)
### 2.1 동시성은 "락"이 아니라 "레이어 방어"로 해결한다
- **(A) Oversell 방지(재고 경쟁)**: Redis 원자 연산 `DECR`
- **(B) Duplicate 방지(1인 1매 경쟁)**: DB Unique `(campaign_id, user_id)`
- **(C) Retry/중복 처리(요청/메시지 중복 경쟁)**: `request_id` 멱등성 (DB Unique + 재조회)

### 2.2 동시성 체크리스트(모든 PR에서 확인)
- [ ] 재고 관련 로직은 **락 없이** Redis 원자 연산으로 처리되는가?
- [ ] 최종 원장(DB)은 **unique 제약으로 중복을 차단**하는가?
- [ ] API 재시도/연타/네트워크 중복을 **requestId 멱등성으로 흡수**하는가?
- [ ] 큐/워커에서 **중복 메시지(재처리)**가 와도 결과가 변하지 않는가?
- [ ] 실패 시 **보상(재고 INCR)** 이 정확히 실행되는가?
- [ ] “부분 성공” 상태에서 사용자가 **상태 조회로** 최종 결과를 확인할 수 있는가?

---

## 3. Global Rules (PR 운영 규칙)
- PR 1개 = 1~2시간 분량 목표 (파일 10~25개 내)
- 각 PR마다 반드시 포함:
  - 구현 범위
  - 완료 조건(acceptance criteria)
  - 테스트(단위/통합)
  - 동시성 체크리스트(해당 PR에 적용되는 항목)
  - Cursor prompt(복붙 실행용)

---

# PR#1 — Bootstrap + Common Web Layer + Health
## 목표
프로젝트 뼈대/컨벤션/공통 웹 레이어 준비.

## 구현 범위
- Spring Boot 프로젝트 생성(Gradle)
- Actuator 추가 (health/liveness/readiness)
- Global Exception Handler + 표준 에러 응답(JSON)
- RequestId 필터(없으면 생성) + MDC 로깅 필드 포함

## 완료 조건
- `GET /actuator/health` = UP
- 모든 에러 응답 포맷이 동일한 JSON 형태
- 로그에 requestId가 항상 포함

## 동시성 체크(해당 PR)
- [ ] (C) requestId 생성/전파가 안정적으로 동작하는가? (중복 처리의 기반)

## Cursor prompt
Spring Boot 3.5 + Java 25 + Gradle로 프로젝트를 만들고,
Actuator, Validation, Web 의존성을 추가해줘.
RequestId를 생성/전파하는 Filter를 만들고(MDC 포함),
@ControllerAdvice로 표준 에러 응답 포맷을 강제해줘.
health endpoint 확인용 테스트를 1개 포함해줘.

---

# PR#2 — Local Infra (docker-compose) + Flyway + Profiles
## 목표
로컬에서 DB/Redis/Kafka 인프라를 즉시 띄울 수 있게.

## 구현 범위
- docker-compose: postgres, redis, redpanda
- `application-local.yml` 설정 분리
- Flyway 적용 (마이그레이션 자동 실행)
- 실행 방법 README에 최소 문장 추가

## 완료 조건
- `docker-compose up -d` 후 `./gradlew bootRun --args='--spring.profiles.active=local'` 실행 성공
- Flyway가 실행되어 스키마 적용 준비 완료

## 동시성 체크(해당 PR)
- [ ] (A)(B)(C)를 위한 인프라가 로컬에서 일관되게 재현 가능한가?

## Cursor prompt
docker-compose.yml에 postgres, redis, redpanda를 추가해줘.
application-local.yml을 만들고 datasource/redis/kafka 설정을 분리해줘.
Flyway를 추가해서 앱 실행 시 마이그레이션이 자동 실행되게 하고,
로컬 실행 방법을 README에 짧게 적어줘.

---

# PR#3 — DB Schema v1 + Repositories + Testcontainers
## 목표
정합성(동시성) 최종 방어선(DB unique) 확정.

## 구현 범위 (Flyway V1__init.sql)
Tables:
- campaigns
  - id (uuid pk)
  - name (varchar)
  - total_quantity (int)
  - status (varchar)
  - start_at, end_at (timestamp)
  - created_at (timestamp)
- coupon_issues
  - id (uuid pk)
  - campaign_id (uuid fk)
  - user_id (varchar)
  - request_id (varchar)
  - status (varchar)
  - created_at, issued_at (timestamp)

Constraints:
- unique(campaign_id, user_id)
- unique(request_id)

Indexes:
- index(campaign_id, created_at)

+ JPA Entity/Repository 작성  
+ Testcontainers 통합 테스트 작성

## 완료 조건
- 마이그레이션 성공
- repository 통합 테스트 2개:
  1) request_id unique 위반
  2) (campaign_id,user_id) unique 위반

## 동시성 체크(해당 PR)
- [ ] (B) `unique(campaign_id, user_id)`가 실제로 중복 발급을 차단하는가?
- [ ] (C) `unique(request_id)`가 멱등성 기반으로 동작하는가?

## Cursor prompt
Flyway 마이그레이션으로 campaigns, coupon_issues 테이블을 만들고,
unique(campaign_id,user_id)와 unique(request_id) 제약 및 인덱스를 추가해줘.
JPA 엔티티와 리포지토리를 작성하고,
Testcontainers(Postgres)로 유니크 제약을 검증하는 통합 테스트 2개를 작성해줘.

---

# PR#4 — Campaign APIs + Activate -> Redis Stock Init
## 목표
재고 초기화(단일 작업) 파이프라인 확보.

## 구현 범위
- POST `/admin/campaigns` (생성)
- PATCH `/admin/campaigns/{id}/activate` (ACTIVE 전환)
  - 전환 시 Redis `stock:{campaignId} = total_quantity` 설정(초기화)
- GET `/campaigns` (목록)

## 완료 조건
- 캠페인 생성 → activate 시 Redis key 생성 확인
- MockMvc 통합 테스트 1개 포함

## 동시성 체크(해당 PR)
- [ ] 재고 초기화는 **단 한 번** 수행되도록 보장되는가? (재실행 시 정책 정의)
  - 권장 정책: 이미 stock 키가 있으면 overwrite 금지 또는 명시적 reset API로 분리

## Cursor prompt
캠페인 생성/조회 API를 구현해줘.
POST /admin/campaigns, GET /campaigns, PATCH /admin/campaigns/{id}/activate를 만들고,
activate 시 Redis에 stock:{campaignId} 키를 total_quantity로 세팅해줘.
MockMvc 통합 테스트로 생성->activate->redis key 존재를 검증해줘.

---

# PR#5 — Claim v0 (DB-only Sync) with Idempotency
## 목표
Redis/Queue 없이도 “중복/멱등”이 정확히 동작하도록.

## 구현 범위
- POST `/campaigns/{id}/claim`
  - requestId 없으면 생성, 있으면 사용
  - DB insert 시도
  - unique(campaign_id,user_id) 위반 -> DUPLICATE
  - unique(request_id) 위반 -> 기존 레코드 조회 후 동일 응답(멱등)
- GET `/claims/{requestId}`

## 완료 조건
통합 테스트 3개:
1) 정상 발급
2) 동일 user 중복 -> DUPLICATE
3) 동일 requestId 재요청 -> 동일 응답(멱등)

## 동시성 체크(해당 PR)
- [ ] (B) 동일 user 동시 요청에서도 최종 1건만 생성되는가? (unique로 보장)
- [ ] (C) 동일 requestId 재시도에도 결과가 변하지 않는가?

## Cursor prompt
DB 기반 동기 발급(Claim v0)를 구현해줘.
POST /campaigns/{id}/claim는 requestId가 없으면 생성해서 반환하고,
coupon_issues에 insert 시도해.
unique(campaign_id,user_id) 위반이면 DUPLICATE 처리하고,
unique(request_id) 위반이면 기존 레코드를 조회해 동일 응답을 반환해(멱등).
GET /claims/{requestId}로 상태 조회 가능하게 하고,
MockMvc 통합 테스트 3개(정상/중복/멱등)를 작성해줘.

---

# PR#6 — Redis Atomic Stock Reserve (Oversell 0) + Fail-safe
## 목표
재고 경쟁(동시성) 1차 방어 완성: 초과발급 0.

## 구현 범위
claim 요청 시:
1) Redis `DECR stock:{campaignId}`로 선차감
2) 결과 < 0 이면 `INCR` 보상 후 SOLD_OUT 응답
3) Redis 장애(타임아웃 포함) 시 503 반환 (정합성 우선)

## 완료 조건
- 멀티스레드 통합 테스트:
  - stock=100, 동시 200 요청
  - 성공 100 / SOLD_OUT 100
- Redis 다운 시 claim이 503

## 동시성 체크(해당 PR)
- [ ] (A) 재고 차감은 반드시 Redis 원자 연산(DECR)인가?
- [ ] (A) 음수 진입 시 보상(INCR)이 정확히 수행되는가?
- [ ] Redis 장애 시 “정합성 우선 fail-safe(503)”가 동작하는가?

## Cursor prompt
Redis 재고 선차감 로직을 Claim에 추가해줘.
DECR stock:{campaignId} 결과가 음수면 INCR로 복구하고 SOLD_OUT 응답을 반환해.
Redis 연결 실패/타임아웃이면 503으로 fail-safe.
병렬 요청 통합 테스트를 작성해 stock=100, 요청=200에서
issued=100, sold_out=100이 되는지 검증해줘.

---

# PR#7 — Async Issuance v1 (Kafka + 202 Accepted + Worker Commit)
## 목표
스파이크 흡수: API는 빠르게 “접수”, 확정은 Worker가 처리.

## 구현 범위
- claim API:
  - Redis 선차감 성공 -> Kafka publish -> 202 반환(requestId)
- worker(consumer):
  - 메시지 수신 후 DB에 insert/업데이트
  - 성공: ISSUED
  - unique(campaign_id,user_id) 위반: DUPLICATE + Redis INCR 보상
  - 발행 실패: Redis INCR 보상 후 503
- 상태 조회는 requestId 기반으로 동작

## 완료 조건
- claim은 즉시 202
- worker 소비 후 상태가 ISSUED로 바뀜
- 중복 발급 시 DUPLICATE + 재고 복구 확인

## 동시성 체크(해당 PR)
- [ ] (A→B) “예약(Redis)”과 “확정(DB)”이 분리되어도 초과/중복이 생기지 않는가?
- [ ] (B) worker가 DB unique 위반을 정확히 처리하는가?
- [ ] 실패 시 보상(INCR)이 누락되지 않는가?
- [ ] 큐 중복/재시도 기반을 위해 requestId가 메시지에 포함되는가?

## Cursor prompt
Kafka 기반 비동기 발급(v1)을 구현해줘.
claim API는 Redis 선차감 성공 시 Kafka(topic: coupon-claims)에 메시지 발행 후 202 반환(requestId 포함).
Consumer worker가 메시지를 받아 DB에 발급을 확정하고 status를 ISSUED로 만든다.
unique(campaign_id,user_id) 위반이면 DUPLICATE 처리하고 Redis 재고를 INCR로 복구해.
메시지 발행 실패 시 INCR 복구 후 503을 반환해.
통합 테스트 2개:
1) claim -> consume -> status ISSUED
2) 중복 요청 -> DUPLICATE & 재고 복구 확인
가능하면 Testcontainers Kafka(또는 Embedded Kafka)를 사용해.

---

# PR#8 — Idempotency Hardening (API + Worker, duplicate messages safe)
## 목표
네트워크 재시도 + Kafka 중복 메시지에서도 결과가 안정적으로 동일.

## 구현 범위
- API 멱등:
  - 동일 requestId로 claim 재호출 시 기존 상태 반환(새 처리 금지)
- Worker 멱등:
  - 동일 requestId 메시지 재처리에도 최종 결과 변동 없음
- (옵션) Redis idempotency:{requestId} TTL 키로 빠른 차단(보조)

## 완료 조건
- 동일 requestId 2회 호출 결과 동일
- 동일 메시지 2회 소비해도 발급 결과/재고가 깨지지 않음(테스트 포함)

## 동시성 체크(해당 PR)
- [ ] (C) requestId 멱등성이 API와 worker 양쪽에서 완결되는가?
- [ ] 중복 메시지에서도 보상(INCR)이 중복 적용되지 않는가?

## Cursor prompt
멱등성을 강화해줘.
API에서 동일 requestId로 claim이 재호출되면 새로운 처리를 하지 말고 기존 상태를 반환해.
Worker에서 동일 requestId 메시지가 중복 처리되어도 결과가 변하지 않게 해줘.
중복 메시지 시뮬레이션 통합 테스트를 추가해줘.

---

# PR#9 — Rate Limiting + Protective Mode
## 목표
연타/봇/폭주로부터 시스템 보호.

## 구현 범위
- Redis 기반 레이트 리밋:
  - user: 5 req / 10s
  - ip: 30 req / 10s
- 보호 모드:
  - 큐 lag 또는 에러율 임계치 초과 시 claim을 503/429로 degrade(설정 기반)

## 완료 조건
- 레이트 리밋 초과 시 429
- 보호 모드 강제 ON 시 claim이 degrade 되는지 테스트

## 동시성 체크(해당 PR)
- [ ] 레이트리밋은 Redis 원자 연산으로 경쟁 조건 없이 동작하는가?
- [ ] 보호 모드가 “폭주 시 더 큰 장애”를 막도록 안전하게 설계되었는가?

## Cursor prompt
Redis 기반 레이트 리밋을 구현해줘(user 5req/10s, ip 30req/10s).
초과 시 429 반환.
또한 queue lag 또는 에러율이 임계치를 넘으면 보호 모드를 켜서 claim을 503 또는 429로 degrade 할 수 있게 설정/코드 구조를 추가해줘.
레이트리밋 동작 통합 테스트 1개 이상 작성해줘.

---

# PR#10 — Observability (Metrics/Logs/Dashboard-ready)
## 목표
“현업 인정” 포인트: 관측 가능성 확보.

## 구현 범위
- Micrometer Prometheus 적용: `/actuator/prometheus`
- 메트릭:
  - `claim_requests_total{result}`
  - 기본 HTTP latency(자동)
  - (옵션) `queue_lag`, `redis_stock_remaining`
- 구조화 로그:
  - requestId, userId, campaignId, result, latencyMs
- docker-compose에 prometheus/grafana 추가(방법 문서)

## 완료 조건
- Prometheus scrape 가능
- Grafana에서 latency/error 확인 가능(최소 구성 가이드)
- README에 스크린샷 섹션 자리 마련

## 동시성 체크(해당 PR)
- [ ] oversell/duplicate/idempotency 결과가 메트릭으로 구분되어 관측되는가?

## Cursor prompt
Micrometer Prometheus를 붙이고 /actuator/prometheus를 노출해줘.
claim 결과별 카운터(claim_requests_total{result})를 추가하고,
로그를 구조화해서 requestId/userId/campaignId/result/latencyMs가 찍히게 해줘.
docker-compose에 prometheus/grafana를 추가하고,
latency/error를 볼 수 있는 최소 구성 방법을 docs에 적어줘.

---

# PR#11 — k6 Load Tests + Verification Workflow
## 목표
실트래픽이 없어도 “검증 가능한 증거” 만들기.

## 구현 범위
- `/loadtest` 폴더에 k6 스크립트 3종:
  - spike
  - soak
  - oversubscription
- 검증 워크플로우 문서:
  - 실행 방법
  - 결과 저장 규칙
  - 합격 기준
- oversubscription 검증 방법 포함:
  - 발급 수 = 재고 수 정확히 일치

## 완료 조건
- k6 실행 가능
- 합격 기준 문서화 완료

## 동시성 체크(해당 PR)
- [ ] (A) oversubscription에서 초과발급 0이 증명되는가?
- [ ] (B) 중복 발급 0이 증명되는가?
- [ ] (C) 재시도/중복 메시지 조건에서도 결과가 안정적인가? (가능하면 추가 시나리오)

## Cursor prompt
/loadtest에 k6 스크립트를 추가해줘.
spike(0->5000rps 10s), soak(2000rps 30m), oversubscription(stock=10000, req=20000) 시나리오를 만들고,
실행 방법과 합격 기준을 docs에 정리해줘.
oversubscription은 최종 발급 수가 정확히 stock과 일치하는지 검증하는 방법(예: 관리자 통계 API 또는 DB 조회)을 포함해줘.

---

# PR#12 — README Final (Portfolio-ready)
## 목표
제출 가능한 포트폴리오 패키지 완성.

## 구현 범위
- Mermaid 아키텍처 다이어그램
- 설계 결정/트레이드오프
- 장애 시나리오 표
- 부하 테스트 결과 템플릿(표/그래프 캡처 자리)
- 개선 전/후 섹션(병목 분석 기록용)

## 완료 조건
- README만 봐도 설계/검증/운영이 이해됨
- 면접 질문 대비 가능

## 동시성 체크(해당 PR)
- [ ] 동시성 전략(Redis 원자 연산 + DB unique + requestId 멱등)이 문서에 명확히 설명되는가?

## Cursor prompt
README를 포트폴리오 제출 수준으로 완성해줘.
mermaid로 아키텍처 다이어그램, 설계 결정/트레이드오프, 장애 시나리오 표,
부하 테스트 결과 템플릿(표/그래프 캡처 자리), 개선 전/후 섹션까지 포함해줘.

---

## Recommended Order
PR#1 → #2 → #3 → #4 → #5 → #6 → #7 → #8 → #9 → #10 → #11 → #12

---

## PR Review Checklist (네가 PR마다 빠르게 보는 용도)
- [ ] 로컬 실행(docker-compose up) 가능한가?
- [ ] 테스트가 통과하는가?
- [ ] (A) Redis 재고 원자 연산/보상이 깨지지 않는가?
- [ ] (B) DB unique로 중복 발급이 차단되는가?
- [ ] (C) requestId 멱등성이 API/Worker 모두에서 성립하는가?
- [ ] 장애 시 안전하게 실패(fail-safe)하는가?
- [ ] 메트릭/로그로 결과를 관측할 수 있는가?