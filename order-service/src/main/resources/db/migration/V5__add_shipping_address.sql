CREATE TABLE IF NOT EXISTS shipping_addresses (
                                                  id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
                                                  first_name  VARCHAR(100) NOT NULL,
                                                  last_name   VARCHAR(100) NOT NULL,
                                                  phone_number       VARCHAR(20),
                                                  street      VARCHAR(255) NOT NULL,
                                                  city        VARCHAR(100) NOT NULL,
                                                  postal_code VARCHAR(20)  NOT NULL,
                                                  country     VARCHAR(2)   NOT NULL,
                                                  created_at   TIMESTAMP    NOT NULL,
                                                  updated_at   TIMESTAMP    NOT NULL
);

ALTER TABLE orders ADD COLUMN IF NOT EXISTS shipping_address_id UUID
    REFERENCES shipping_addresses(id) ON DELETE SET NULL;

ALTER TABLE orders DROP COLUMN IF EXISTS shipping_address;