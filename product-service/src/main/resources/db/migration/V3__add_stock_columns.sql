ALTER TABLE product
    ADD COLUMN quantity          INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN reserved_quantity INTEGER NOT NULL DEFAULT 0,
    ADD CONSTRAINT chk_quantity_non_negative CHECK (quantity >= 0),
    ADD CONSTRAINT chk_reserved_quantity_non_negative CHECK (reserved_quantity >= 0),
    ADD CONSTRAINT chk_reserved_not_exceeding CHECK (reserved_quantity <= quantity);

-- Les produits existants ont un stock inconnu ; on les marque hors stock.
-- L'admin devra définir les quantités après migration.
UPDATE product
SET in_stock = FALSE
WHERE quantity = 0;

CREATE TABLE stock_reservation
(
    order_id    UUID PRIMARY KEY,
    reserved_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE stock_reservation_item
(
    order_id   UUID    NOT NULL REFERENCES stock_reservation (order_id) ON DELETE CASCADE,
    product_id BIGINT  NOT NULL REFERENCES product (id),
    quantity   INTEGER NOT NULL,
    PRIMARY KEY (order_id, product_id)
);
