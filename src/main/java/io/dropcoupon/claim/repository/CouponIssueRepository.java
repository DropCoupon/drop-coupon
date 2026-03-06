package io.dropcoupon.claim.repository;

import io.dropcoupon.claim.domain.CouponIssue;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CouponIssueRepository extends JpaRepository<CouponIssue, UUID> {

    Optional<CouponIssue> findByRequestId(String requestId);

    Optional<CouponIssue> findByCampaignIdAndUserId(UUID campaignId, String userId);
}
