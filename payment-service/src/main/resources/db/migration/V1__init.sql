CREATE TABLE processed_events (
    event_id     UUID PRIMARY KEY,
    event_type   VARCHAR(100) NOT NULL,
    processed_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE TABLE payment (
    id                UUID PRIMARY KEY,
    order_id          UUID NOT NULL UNIQUE,
    user_id           VARCHAR(50) NOT NULL,
    amount            NUMERIC(10,2) NOT NULL CHECK (amount > 0),
    status            VARCHAR(30) NOT NULL,
    payment_reference VARCHAR(50),
    failure_reason    TEXT,
    created_at        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX idx_payment_order_id ON payment(order_id);
CREATE INDEX idx_payment_status ON payment(status);
