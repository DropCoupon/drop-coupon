SSE 엔드포인트를 구현해줘: GET /claims/{requestId}/stream
규칙:
- 연결 즉시 현재 상태 1회 전송(event: status)
- heartbeat 15초(event: heartbeat)
- 최종 상태면 event: done 전송 후 종료
- 최대 연결 2분, 타임아웃 시 done + message='timeout' 후 종료
- SSE 불가 환경을 위해 GET /claims/{requestId} 폴링 API는 유지
필요한 서비스/저장 방식(상태 조회)은 PR 범위 내에서 최소로 구현해줘.
