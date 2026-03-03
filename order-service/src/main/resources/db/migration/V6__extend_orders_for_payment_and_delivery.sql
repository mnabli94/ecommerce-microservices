ALTER TABLE orders
    DROP CONSTRAINT IF EXISTS orders_status_check;

ALTER TABLE orders
    ALTER COLUMN status TYPE VARCHAR(30) USING (status::VARCHAR(30));


ALTER TABLE orders
    ADD COLUMN IF NOT EXISTS contact_email VARCHAR(254);
ALTER TABLE orders
    ADD COLUMN IF NOT EXISTS payment_method_id VARCHAR(50);
ALTER TABLE orders
    ADD COLUMN IF NOT EXISTS payment_reference VARCHAR(50);
ALTER TABLE orders
    ADD COLUMN IF NOT EXISTS cancellation_reason VARCHAR(255);


ALTER TABLE orders
    ADD COLUMN IF NOT EXISTS confirmed_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE orders
    ADD COLUMN IF NOT EXISTS shipped_at TIMESTAMP WITH TIME ZONE;

ALTER TABLE orders
    ADD CONSTRAINT uc_orders_shipping_address UNIQUE (shipping_address_id);

