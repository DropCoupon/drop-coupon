# Performance & Load Testing Plan

---

## 1. SLO Targets

- 5,000 RPS peak
- 10,000 concurrent users
- P95 < 200ms
- Oversell = 0
- Error rate < 1%

---

## 2. Test Scenarios

### Spike Test
Ramp 0 → 5,000 RPS in 10s

### Soak Test
2,000 RPS for 30 minutes

### Oversubscription Test
Stock = 10,000
Requests = 20,000
Expected issued = 10,000

---

## 3. Metrics to Collect

- RPS
- P95 latency
- Error rate
- Redis ops/sec
- DB connection usage
- Queue lag

---

## 4. Success Criteria

- No oversell
- Stable latency
- No resource exhaustion