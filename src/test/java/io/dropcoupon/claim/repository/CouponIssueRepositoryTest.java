package io.dropcoupon.claim.repository;

import io.dropcoupon.campaign.domain.Campaign;
import io.dropcoupon.campaign.repository.CampaignRepository;
import io.dropcoupon.claim.domain.CouponIssue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(
    properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration,org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.jpa.open-in-view=false"
    }
)
@Testcontainers(disabledWithoutDocker = true)
class CouponIssueRepositoryTest {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"))
        .withDatabaseName("dropcoupon")
        .withUsername("test")
        .withPassword("test");

    @DynamicPropertySource
    static void registerDatasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private CouponIssueRepository couponIssueRepository;

    @Autowired
    private CampaignRepository campaignRepository;

    private UUID campaignId;

    @BeforeEach
    void setUp() {
        couponIssueRepository.deleteAll();
        campaignRepository.deleteAll();

        Campaign campaign = new Campaign(
            "테스트 캠페인",
            100,
            Instant.now(),
            Instant.now().plus(7, ChronoUnit.DAYS)
        );
        campaignRepository.save(campaign);
        campaignId = campaign.getId();
    }

    @Test
    @DisplayName("동일 campaign_id + user_id 조합으로 중복 발급 시 DataIntegrityViolationException 발생")
    void duplicateCampaignUserThrowsException() {
        CouponIssue first = new CouponIssue(campaignId, "user-1", "req-1");
        couponIssueRepository.saveAndFlush(first);

        CouponIssue duplicate = new CouponIssue(campaignId, "user-1", "req-2");

        assertThatThrownBy(() -> couponIssueRepository.saveAndFlush(duplicate))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("동일 request_id로 중복 저장 시 DataIntegrityViolationException 발생")
    void duplicateRequestIdThrowsException() {
        CouponIssue first = new CouponIssue(campaignId, "user-1", "req-same");
        couponIssueRepository.saveAndFlush(first);

        CouponIssue duplicate = new CouponIssue(campaignId, "user-2", "req-same");

        assertThatThrownBy(() -> couponIssueRepository.saveAndFlush(duplicate))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("requestId로 발급 내역을 조회할 수 있다")
    void findByRequestId() {
        CouponIssue issue = new CouponIssue(campaignId, "user-1", "req-find");
        couponIssueRepository.saveAndFlush(issue);

        var found = couponIssueRepository.findByRequestId("req-find");

        assertThat(found).isPresent();
        assertThat(found.get().getUserId()).isEqualTo("user-1");
    }
}
