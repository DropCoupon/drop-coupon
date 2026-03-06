CREATE TABLE campaigns (
    id         UUID         PRIMARY KEY,
    name       VARCHAR(255) NOT NULL,
    total_quantity INT       NOT NULL,
    status     VARCHAR(20)  NOT NULL DEFAULT 'DRAFT',
    start_at   TIMESTAMP    NOT NULL,
    end_at     TIMESTAMP    NOT NULL,
    created_at TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE TABLE coupon_issues (
    id          UUID         PRIMARY KEY,
    campaign_id UUID         NOT NULL REFERENCES campaigns(id),
    user_id     VARCHAR(255) NOT NULL,
    request_id  VARCHAR(255) NOT NULL,
    status      VARCHAR(20)  NOT NULL DEFAULT 'REQUESTED',
    created_at  TIMESTAMP    NOT NULL DEFAULT now(),
    issued_at   TIMESTAMP,

    CONSTRAINT uk_coupon_issues_campaign_user UNIQUE (campaign_id, user_id),
    CONSTRAINT uk_coupon_issues_request_id    UNIQUE (request_id)
);

CREATE INDEX idx_coupon_issues_campaign_created ON coupon_issues (campaign_id, created_at);
