# API Contract & Operational Defaults (Recommended)
대용량 트래픽 대응 선착순 쿠폰 발급 시스템 — 권장값 확정본 v1.0

본 문서는 Phase B 구현을 시작하기 위한 “마지막 고정값”을 정의한다.
(상태 모델, SSE 스펙, 보호모드 트리거)

---

## 1) SSE 엔드포인트 스펙 (권장)
### 1.1 Endpoint
- `GET /claims/{requestId}/stream`

### 1.2 목적
- Claim 요청 후 최종 결과(ISSUED/DUPLICATE/SOLD_OUT/FAILED)를
  **롱폴링 대신 SSE로 전달**한다.
- SSE가 불가한 환경을 위해 `GET /claims/{requestId}` 폴링 API는 유지한다.

### 1.3 Response (SSE)
- Content-Type: `text/event-stream`
- Cache-Control: `no-cache`
- Connection: `keep-alive`

#### 이벤트 타입
- `event: status`  (상태 변경/현재 상태 전송)
- `event: heartbeat` (연결 유지)
- `event: done` (최종 상태 도달 → 서버가 연결 종료 권장)

#### 데이터 포맷(JSON)
```json
{
  "requestId": "req_...",
  "campaignId": "uuid",
  "userId": "user-123",
  "status": "PENDING|ISSUED|DUPLICATE|SOLD_OUT|FAILED",
  "message": "optional",
  "updatedAt": "2026-02-26T12:34:56Z"
}