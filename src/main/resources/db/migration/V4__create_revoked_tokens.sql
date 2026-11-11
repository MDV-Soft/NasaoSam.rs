-- Opozvani (odjavljeni) tokeni. Svaki token ima jedinstven ID (jti), pa se čuva samo on.
-- Token nakon isteka ionako ne važi, zato se pamti i expires_at: posle tog trenutka
-- red više ne služi ničemu i može da se obriše (zakazani posao, faza 8).
CREATE TABLE revoked_tokens (
                                jti        CHAR(36)    NOT NULL,
                                expires_at DATETIME(6) NOT NULL,
                                revoked_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    -- Svaki zahtev proverava da li je njegov token ovde, pa mora biti brzo
                                PRIMARY KEY (jti),
    -- Za brisanje isteklih redova
                                KEY idx_revoked_tokens_expires_at (expires_at)
);