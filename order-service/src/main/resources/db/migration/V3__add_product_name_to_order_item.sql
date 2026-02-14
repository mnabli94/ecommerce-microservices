ALTER TABLE order_item ADD COLUMN product_name VARCHAR(255);
UPDATE order_item SET product_name = 'Unknown' WHERE product_name IS NULL;
ALTER TABLE order_item ALTER COLUMN product_name SET NOT NULL;
