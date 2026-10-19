CREATE TABLE appconfig.mini_app (
    id                 BIGSERIAL PRIMARY KEY,
    code               VARCHAR(30)  NOT NULL UNIQUE,
    title              VARCHAR(50)  NOT NULL,
    icon               VARCHAR(50)  NOT NULL,
    required_kyc_level INTEGER      NOT NULL CHECK (required_kyc_level BETWEEN 0 AND 2),
    enabled            BOOLEAN      NOT NULL,
    sort_order         INTEGER      NOT NULL,
    created_date       TIMESTAMPTZ  NOT NULL,
    updated_date       TIMESTAMPTZ,
    version            BIGINT       NOT NULL DEFAULT 0
);

CREATE TABLE appconfig.feature_flag (
    id           BIGSERIAL PRIMARY KEY,
    code         VARCHAR(50)  NOT NULL UNIQUE,
    enabled      BOOLEAN      NOT NULL,
    description  VARCHAR(200),
    created_date TIMESTAMPTZ  NOT NULL,
    updated_date TIMESTAMPTZ,
    version      BIGINT       NOT NULL DEFAULT 0
);

-- Market iOS'ta henüz yok; kapalı kayıt, uygulamanın tanımadığı mini app'i göstermediğini denemek için
INSERT INTO appconfig.mini_app (code, title, icon, required_kyc_level, enabled, sort_order, created_date)
VALUES ('FOOD', 'Yemek', 'fork.knife', 0, TRUE, 1, now()),
       ('TAXI', 'Taksi', 'car.fill', 0, TRUE, 2, now()),
       ('MARKET', 'Market', 'basket.fill', 0, FALSE, 3, now());

INSERT INTO appconfig.feature_flag (code, enabled, description, created_date)
VALUES ('QR_PAYMENT', FALSE, 'TR Karekod ile ödeme (CP-5)', now()),
       ('CREDIT', FALSE, 'Finans sekmesi ve sonra öde (CP-7)', now()),
       ('CERTIFICATE_PINNING', FALSE, 'iOS sertifika sabitleme', now());
