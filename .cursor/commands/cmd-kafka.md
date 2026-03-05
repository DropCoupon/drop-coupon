Kafka(로컬 Redpanda) 기반으로 topic 'coupon-claims'를 사용해
producer/consumer를 최소 범위로 구현해줘.
- 메시지에 requestId, campaignId, userId 포함
- key는 requestId
- consumer는 멱등하게 동작하도록 설계(중복 메시지 안전)
- 실패 시 보상(INCR)이 중복 적용되지 않게 주의
- Testcontainers Kafka 가능하면 사용, 어렵다면 대체 전략 제시
