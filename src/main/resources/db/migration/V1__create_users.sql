CREATE TABLE users (
   id             BIGINT       NOT NULL AUTO_INCREMENT,
   email          VARCHAR(255) NOT NULL,
   password_hash  VARCHAR(255) NULL,
   display_name   VARCHAR(100) NULL,
   role           VARCHAR(20)  NOT NULL,
   email_verified BOOLEAN      NOT NULL DEFAULT FALSE,
   created_at     DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
   updated_at     DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
   PRIMARY KEY (id),
   UNIQUE KEY uk_users_email (email)
);