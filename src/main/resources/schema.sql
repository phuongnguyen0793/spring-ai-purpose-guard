-- Executed by Spring Boot on startup (spring.sql.init.mode=always).
-- Idempotent: safe to run on every boot.

-- Business purposes for semantic search. The embedding column stores the
-- vector as a JSON array of floats; it is computed once (via Spring AI's
-- EmbeddingModel) and then reused on subsequent startups.
CREATE TABLE IF NOT EXISTS business_purpose (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    purpose_text TEXT        NOT NULL,
    embedding    LONGTEXT    NULL,
    created_at   TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Trained (industry_id, business_type, company_name, relation) rows loaded
-- from the offline-generated CSV. data_version tracks the CSV batch a row
-- came from so new CSV batches can be loaded incrementally.
CREATE TABLE IF NOT EXISTS company_name_rule (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    industry_id   VARCHAR(64)  NOT NULL,
    business_type VARCHAR(128) NOT NULL,
    company_name  VARCHAR(255) NOT NULL,
    relation      VARCHAR(32)  NOT NULL,
    data_version  INT          NOT NULL,
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_rule_industry_business (industry_id, business_type),
    INDEX idx_rule_data_version (data_version)
);

-- Tracks which CSV data versions have already been imported per dataset,
-- so RuleDataLoader only ingests newly-added rows.
CREATE TABLE IF NOT EXISTS data_version (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    dataset    VARCHAR(64) NOT NULL,
    version    INT         NOT NULL,
    row_count  INT         NOT NULL DEFAULT 0,
    loaded_at  TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_dataset_version (dataset, version)
);
