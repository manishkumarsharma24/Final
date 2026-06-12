-- V3: Convert CHAR columns to VARCHAR to match Hibernate entity mappings.
-- PostgreSQL stores CHAR(n) as bpchar; Hibernate 6 expects varchar for @Column(length=n).

ALTER TABLE customers
    ALTER COLUMN country TYPE VARCHAR(2);

ALTER TABLE orders
    ALTER COLUMN ship_country TYPE VARCHAR(2);

ALTER TABLE products
    ALTER COLUMN currency TYPE VARCHAR(3);

ALTER TABLE order_items
    ALTER COLUMN currency TYPE VARCHAR(3);
