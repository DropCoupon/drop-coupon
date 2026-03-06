package io.dropcoupon.claim.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * 쿠폰 발급 내역 엔티티.
 * unique(campaign_id, user_id)로 1인 1매(B), unique(request_id)로 멱등성(C)을 보장한다.
 */
@Entity
@Table(name = "coupon_issues")
public class CouponIssue {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "campaign_id", nullable = false)
    private UUID campaignId;

    @Column(name = "user_id", nullable = false, length = 255)
    private String userId;

    @Column(name = "request_id", nullable = false, length = 255)
    private String requestId;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private CouponIssueStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "issued_at")
    private Instant issuedAt;

    protected CouponIssue() {
    }

    public CouponIssue(UUID campaignId, String userId, String requestId) {
        this.id = UUID.randomUUID();
        this.campaignId = campaignId;
        this.userId = userId;
        this.requestId = requestId;
        this.status = CouponIssueStatus.REQUESTED;
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getCampaignId() { return campaignId; }
    public String getUserId() { return userId; }
    public String getRequestId() { return requestId; }
    public CouponIssueStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getIssuedAt() { return issuedAt; }

    public void markIssued() {
        this.status = CouponIssueStatus.ISSUED;
        this.issuedAt = Instant.now();
    }
}
