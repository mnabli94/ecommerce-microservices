ALTER TABLE orders
    ADD COLUMN retry_count INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN expires_at  TIMESTAMP WITH TIME ZONE;

UPDATE orders
SET expires_at = CASE
                     WHEN status IN ('PENDING', 'AWAITING_PAYMENT')
                         THEN NOW() + INTERVAL '15 minutes'
                     ELSE created_at + INTERVAL '15 minutes'
    END;
