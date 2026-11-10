
-- Telefon korisnika, sifrovan AES-om
-- moze biti NULL jer nalozi napravljeni pre ove migracije nemaju telefon;
-- obavezan je samo pri novoj registraciji (proverava ga RegisterRequest).
ALTER TABLE users
    ADD COLUMN phone TEXT NULL AFTER display_name;