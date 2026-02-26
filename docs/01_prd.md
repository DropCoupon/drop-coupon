# Coupon Issuance System
## Product Requirements Document (PRD)

---

## 1. Overview

This project aims to build a high-concurrency coupon issuance system
that guarantees data consistency under heavy traffic spikes.

Primary goal:
- Prevent overselling
- Prevent duplicate issuance
- Handle up to 5,000 RPS
- Maintain P95 latency under 200ms

---

## 2. Problem Statement

During event openings, traffic spikes cause:
- DB lock contention
- Overselling
- Duplicate issuance
- Connection pool exhaustion
- Service downtime

This system is designed specifically to handle those scenarios.

---

## 3. Scope

### Included
- Coupon campaign creation
- First-come-first-served issuance
- Duplicate prevention
- Inventory management
- Async processing
- Rate limiting
- Load testing validation

### Excluded
- Payment integration
- Marketing system
- Advanced bot detection

---

## 4. Functional Requirements

### User
- Claim coupon
- Check claim status
- View issued coupons

### Admin
- Create campaign
- Set inventory
- Change campaign status

---

## 5. Non-Functional Requirements

- Stateless API servers
- Horizontal scalability
- Zero oversell
- Zero duplicate issuance
- Observability enabled