보호모드를 구현해줘.
- 설정: protection.mode.enabled (기본 false)
- ON이면 claim API는 503 반환
- 응답 메시지는 표준 에러 포맷으로
- 테스트 1개 포함(보호모드 ON일 때 503 확인)
가능하면 이후에 lag 기반 자동 트리거로 확장할 수 있는 구조로 만들어줘.
