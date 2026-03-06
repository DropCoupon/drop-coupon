package io.dropcoupon.campaign.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * 쿠폰 캠페인(재고/기간) 엔티티.
 */
@Entity
@Table(name = "campaigns")
public class Campaign {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(name = "total_quantity", nullable = false)
    private int totalQuantity;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private CampaignStatus status;

    @Column(name = "start_at", nullable = false)
    private Instant startAt;

    @Column(name = "end_at", nullable = false)
    private Instant endAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Campaign() {
    }

    public Campaign(String name, int totalQuantity, Instant startAt, Instant endAt) {
        this.id = UUID.randomUUID();
        this.name = name;
        this.totalQuantity = totalQuantity;
        this.status = CampaignStatus.DRAFT;
        this.startAt = startAt;
        this.endAt = endAt;
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public int getTotalQuantity() { return totalQuantity; }
    public CampaignStatus getStatus() { return status; }
    public Instant getStartAt() { return startAt; }
    public Instant getEndAt() { return endAt; }
    public Instant getCreatedAt() { return createdAt; }
}
