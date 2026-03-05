# System Architecture Design

---

## 1. High-Level Architecture

Client
→ Load Balancer
→ API Server
→ Redis
→ Message Queue
→ Worker
→ PostgreSQL

---

## 2. Claim Flow

1. Rate limit check
2. Idempotency check
3. Redis atomic stock decrement
4. If success → enqueue
5. Immediate 202 response
6. Worker persists issuance

---

## 3. Consistency Strategy

Layer 1: Redis atomic decrement
Layer 2: DB unique constraint (campaign_id, user_id)
Layer 3: Idempotency via request_id

---

## 4. Failure Handling

Redis failure → temporarily disable claim
MQ failure → restore stock and return error
DB failure → queue retry
Worker crash → monitor lag

---

## 5. Data Model

### campaigns
- id
- total_quantity
- status
- start_at
- end_at

### coupon_issues
- id
- campaign_id
- user_id
- request_id
- status

Constraints:
- unique(campaign_id, user_id)
- unique(request_id)