CREATE TABLE found_reports (
                               id                 BIGINT        NOT NULL AUTO_INCREMENT,
    -- CITIZEN/INSTITUTION
                               source             VARCHAR(20)   NOT NULL,

    -- PENDING_EMAIL, ACTIVE, RETURNED, EXPIRED, WITHDRAWN
                               status             VARCHAR(20)   NOT NULL,

                               finder_email       TEXT          NOT NULL,
                               finder_phone       TEXT          NULL,

                               share_email        BOOLEAN       NOT NULL DEFAULT TRUE,
                               share_phone        BOOLEAN       NOT NULL DEFAULT FALSE,

                               found_at           DATETIME(6)   NOT NULL,
                               city               VARCHAR(100)  NOT NULL,
                               place_description  VARCHAR(255)  NULL,
                               latitude           DECIMAL(9,6)  NOT NULL,
                               longitude          DECIMAL(9,6)  NOT NULL,

                               item_location      VARCHAR(20)   NOT NULL,
                               handover_info      VARCHAR(255)  NULL,

    -- AES
                               control_question   VARCHAR(255)  NOT NULL,
                               control_answer     TEXT          NOT NULL,

                               blurred_photo_key  VARCHAR(255)  NULL,

                               expires_at         DATETIME(6)   NOT NULL,
                               activated_at       DATETIME(6)   NULL,
                               closed_at          DATETIME(6)   NULL,
                               created_at         DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                               updated_at         DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

                               PRIMARY KEY (id),

                               CONSTRAINT chk_found_reports_share_something CHECK (share_email OR share_phone),
                               CONSTRAINT chk_found_reports_share_phone CHECK (NOT share_phone OR finder_phone IS NOT NULL),
                               CONSTRAINT chk_found_reports_handover CHECK (item_location = 'WITH_FINDER' OR handover_info IS NOT NULL),

                               KEY idx_found_reports_status_found_at (status, found_at),
                               KEY idx_found_reports_status_expires_at (status, expires_at)
);

CREATE TABLE found_report_items (
                                    id                BIGINT        NOT NULL AUTO_INCREMENT,
                                    found_report_id   BIGINT        NOT NULL,

                                    category          VARCHAR(30)   NOT NULL,
                                    custom_category   VARCHAR(50)   NULL,
                                    document_type     VARCHAR(30)   NULL,
                                    color             VARCHAR(20)   NULL,
                                    brand             VARCHAR(100)  NULL,
                                    material          VARCHAR(50)   NULL,

                                    description       TEXT          NULL,
                                    distinctive_marks VARCHAR(500)  NULL,

    -- HMAC hash za imena za dokumenta
                                    name_hash         CHAR(64)      NULL,
                                    name_dob_hash     CHAR(64)      NULL,

                                    created_at        DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

                                    PRIMARY KEY (id),
                                    CONSTRAINT fk_found_report_items_report FOREIGN KEY (found_report_id) REFERENCES found_reports (id) ON DELETE CASCADE,

                                    KEY idx_found_report_items_report (found_report_id),
                                    KEY idx_found_report_items_category (category),
                                    KEY idx_found_report_items_name_hash (name_hash),
                                    KEY idx_found_report_items_name_dob_hash (name_dob_hash)
);

-- Jednokratni tokeni za linkove iz mejla.
-- Nalzac nema nalog, pa mu link sa tajnom vrednoscu sluzi kao dokaz da je on podneo nalaz.
-- U bazi je samo hash tokena (SHA-256)
CREATE TABLE one_time_tokens (
                                 id              BIGINT        NOT NULL AUTO_INCREMENT,

    -- sada samo FINDER_ACCESS
                                 purpose         VARCHAR(30)   NOT NULL,
                                 token_hash      CHAR(64)      NOT NULL,
                                 found_report_id BIGINT        NOT NULL,

                                 expires_at      DATETIME(6)   NOT NULL,
                                 used_at         DATETIME(6)   NULL,
                                 created_at      DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

                                 PRIMARY KEY (id),
                                 UNIQUE KEY uk_one_time_tokens_hash (token_hash),
                                 CONSTRAINT fk_one_time_tokens_report FOREIGN KEY (found_report_id) REFERENCES found_reports (id) ON DELETE CASCADE,

                                 KEY idx_one_time_tokens_report (found_report_id),
                                 KEY idx_one_time_tokens_expires_at (expires_at)
);
