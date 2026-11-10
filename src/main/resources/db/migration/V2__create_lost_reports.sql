-- Jedan oglas - jedan dogadjaj izgubljenog dogadjaj
CREATE TABLE lost_reports (
                              id                     BIGINT        NOT NULL AUTO_INCREMENT,
                              owner_id               BIGINT        NOT NULL,
                              status                 VARCHAR(20)   NOT NULL,

    -- Vremenska razlika gubitka u UTC-u ako vlasnik zna  vreme, oba su ista
                              lost_from              DATETIME(6)   NOT NULL,
                              lost_to                DATETIME(6)   NOT NULL,

    -- Mesto: grad je obavezan, ostalo pomaze spajanju kad postoji
                              city                   VARCHAR(100)  NOT NULL,
                              place_description      VARCHAR(255)  NULL,
                              latitude               DECIMAL(9,6)  NULL,
                              longitude              DECIMAL(9,6)  NULL,
                              location_radius_meters INT           NULL,

    -- Detalj koji zna samo vlasnik sifrovano sa AES algoritmom
                              private_details        TEXT          NULL,

                              completeness_score     INT           NOT NULL DEFAULT 0,
                              expires_at             DATETIME(6)   NOT NULL,
                              closed_at              DATETIME(6)   NULL,
                              created_at             DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                              updated_at             DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

                              PRIMARY KEY (id),
                              CONSTRAINT fk_lost_reports_owner FOREIGN KEY (owner_id) REFERENCES users (id),
                              CONSTRAINT chk_lost_reports_time_range CHECK (lost_from <= lost_to),

    -- "Moji oglasi"
                              KEY idx_lost_reports_owner (owner_id),
    -- Spajanje: samo aktivni oglasi u datom nekom periodu
                              KEY idx_lost_reports_status_time (status, lost_from, lost_to)
);

-- Jedan oglas moze imati vise predmeta  ----------- TABELA NOVA U BAZI SA PREDEMTIMA
CREATE TABLE lost_report_items (
                                   id                BIGINT        NOT NULL AUTO_INCREMENT,
                                   lost_report_id    BIGINT        NOT NULL,

                                   category          VARCHAR(30)   NOT NULL,
                                   custom_category   VARCHAR(50)   NULL,
                                   document_type     VARCHAR(30)   NULL,
                                   color             VARCHAR(20)   NULL,
                                   brand             VARCHAR(100)  NULL,
                                   material          VARCHAR(50)   NULL,

    -- Detaljan opis vlasnika, glavni tekst koji AI cita pri spajanju
                                   description       TEXT          NULL,
                                   distinctive_marks VARCHAR(500)  NULL,

    -- HMAC hesevi imena sa dokumenta (64 heksadecimalna znaka), samo za dokumenta
                                   name_hash         CHAR(64)      NULL,
                                   name_dob_hash     CHAR(64)      NULL,

                                   created_at        DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

                                   PRIMARY KEY (id),
                                   CONSTRAINT fk_lost_report_items_report FOREIGN KEY (lost_report_id) REFERENCES lost_reports (id) ON DELETE CASCADE,

                                   KEY idx_lost_report_items_report (lost_report_id),
                                   KEY idx_lost_report_items_category (category),
    -- Brza pretraga po imenu, u celoj bazi
                                   KEY idx_lost_report_items_name_hash (name_hash),
                                   KEY idx_lost_report_items_name_dob_hash (name_dob_hash)
);