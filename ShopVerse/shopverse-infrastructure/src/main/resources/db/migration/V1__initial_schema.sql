-- Ch05-01: Core schema — sequences, tables, FK constraints
-- Ch05-03: Indexes for query performance

-- Enable pg_trgm for full-text search on product names (Ch08-03)
CREATE EXTENSION IF NOT EXISTS pg_trgm;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ─── Sequences ────────────────────────────────────────────────────────────────
CREATE SEQUENCE IF NOT EXISTS customers_id_seq START 1 INCREMENT 50;
CREATE SEQUENCE IF NOT EXISTS products_id_seq  START 1 INCREMENT 50;
CREATE SEQUENCE IF NOT EXISTS orders_id_seq    START 1 INCREMENT 50;
CREATE SEQUENCE IF NOT EXISTS order_items_id_seq START 1 INCREMENT 100;

-- ─── Customers ────────────────────────────────────────────────────────────────
CREATE TABLE customers (
    id             BIGINT PRIMARY KEY DEFAULT nextval('customers_id_seq'),
    first_name     VARCHAR(100) NOT NULL,
    last_name      VARCHAR(100) NOT NULL,
    email          VARCHAR(255) NOT NULL UNIQUE,
    phone          VARCHAR(20),
    street         VARCHAR(255),
    city           VARCHAR(100),
    state          VARCHAR(100),
    postal_code    VARCHAR(20),
    country        VARCHAR(2),
    tier           VARCHAR(20) NOT NULL DEFAULT 'STANDARD',
    loyalty_points INT         NOT NULL DEFAULT 0,
    date_of_birth  DATE,
    active         BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX idx_customers_email ON customers(email);
CREATE INDEX idx_customers_tier       ON customers(tier) WHERE active = TRUE;

-- ─── Products ─────────────────────────────────────────────────────────────────
CREATE TABLE products (
    id             BIGINT PRIMARY KEY DEFAULT nextval('products_id_seq'),
    name           VARCHAR(255) NOT NULL,
    description    TEXT,
    price          NUMERIC(12,2) NOT NULL CHECK (price >= 0),
    currency       VARCHAR(3)    NOT NULL DEFAULT 'USD',
    stock_quantity INT           NOT NULL DEFAULT 0 CHECK (stock_quantity >= 0),
    category       VARCHAR(100),
    active         BOOLEAN       NOT NULL DEFAULT TRUE,
    version        BIGINT        NOT NULL DEFAULT 0,
    created_at     TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

-- Ch05-03: B-tree + GIN trigram indexes
CREATE INDEX idx_products_category ON products(category);
CREATE INDEX idx_products_active   ON products(active) WHERE active = TRUE;
CREATE INDEX idx_products_name_trgm ON products USING GIN (name gin_trgm_ops);

-- ─── Orders ───────────────────────────────────────────────────────────────────
CREATE TABLE orders (
    id              BIGINT PRIMARY KEY DEFAULT nextval('orders_id_seq'),
    customer_id     BIGINT       NOT NULL REFERENCES customers(id),
    status          VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    ship_street     VARCHAR(255),
    ship_city       VARCHAR(100),
    ship_state      VARCHAR(100),
    ship_postal_code VARCHAR(20),
    ship_country    VARCHAR(2),
    tracking_number VARCHAR(100),
    version         BIGINT       NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_orders_customer_id ON orders(customer_id);
CREATE INDEX idx_orders_status      ON orders(status);
CREATE INDEX idx_orders_created_at  ON orders(created_at DESC);

-- ─── Order Items ──────────────────────────────────────────────────────────────
CREATE TABLE order_items (
    id           BIGINT PRIMARY KEY DEFAULT nextval('order_items_id_seq'),
    order_id     BIGINT        NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    product_id   BIGINT        NOT NULL REFERENCES products(id),
    product_name VARCHAR(255)  NOT NULL,
    quantity     INT           NOT NULL CHECK (quantity > 0),
    unit_price   NUMERIC(12,2) NOT NULL,
    currency     VARCHAR(3)    NOT NULL DEFAULT 'USD'
);

CREATE INDEX idx_order_items_order_id   ON order_items(order_id);
CREATE INDEX idx_order_items_product_id ON order_items(product_id);

-- Ch05-07: Audit trigger function
CREATE OR REPLACE FUNCTION update_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_products_updated_at
    BEFORE UPDATE ON products
    FOR EACH ROW EXECUTE FUNCTION update_updated_at();

CREATE TRIGGER trg_orders_updated_at
    BEFORE UPDATE ON orders
    FOR EACH ROW EXECUTE FUNCTION update_updated_at();
