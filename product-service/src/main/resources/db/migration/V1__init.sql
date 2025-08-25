CREATE TABLE category (
                          id BIGSERIAL PRIMARY KEY,
                          name VARCHAR(255) NOT NULL UNIQUE
);

CREATE TABLE product (
                         id BIGSERIAL PRIMARY KEY,
                         name VARCHAR(255) NOT NULL,
                         price NUMERIC(19,2) NOT NULL CHECK (price >= 0),
                         in_stock BOOLEAN NOT NULL DEFAULT TRUE,
                         category_id BIGINT,
                         CONSTRAINT fk_product_category FOREIGN KEY (category_id) REFERENCES category(id)
);

CREATE INDEX idx_product_name ON product(name);
CREATE INDEX idx_product_category ON product(category_id);
