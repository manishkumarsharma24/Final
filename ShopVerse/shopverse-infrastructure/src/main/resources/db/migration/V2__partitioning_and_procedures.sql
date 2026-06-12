-- Ch05-08: Table partitioning — partition orders by created_at (range)
-- Ch05-07: Stored procedures for batch operations

-- ─── Partitioned order_audit table ────────────────────────────────────────────
CREATE TABLE order_audit (
    id          BIGSERIAL,
    order_id    BIGINT      NOT NULL,
    old_status  VARCHAR(20),
    new_status  VARCHAR(20) NOT NULL,
    changed_by  VARCHAR(255),
    changed_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
) PARTITION BY RANGE (changed_at);

-- Ch05-08: Monthly partitions for current year
CREATE TABLE order_audit_2024_q1 PARTITION OF order_audit
    FOR VALUES FROM ('2024-01-01') TO ('2024-04-01');
CREATE TABLE order_audit_2024_q2 PARTITION OF order_audit
    FOR VALUES FROM ('2024-04-01') TO ('2024-07-01');
CREATE TABLE order_audit_2024_q3 PARTITION OF order_audit
    FOR VALUES FROM ('2024-07-01') TO ('2024-10-01');
CREATE TABLE order_audit_2024_q4 PARTITION OF order_audit
    FOR VALUES FROM ('2024-10-01') TO ('2025-01-01');
CREATE TABLE order_audit_2025    PARTITION OF order_audit
    FOR VALUES FROM ('2025-01-01') TO ('2026-01-01');
CREATE TABLE order_audit_2026    PARTITION OF order_audit
    FOR VALUES FROM ('2026-01-01') TO ('2027-01-01');
CREATE TABLE order_audit_default PARTITION OF order_audit DEFAULT;

-- Ch05-07: Trigger to populate audit table on order status change
CREATE OR REPLACE FUNCTION audit_order_status_change()
RETURNS TRIGGER AS $$
BEGIN
    IF OLD.status IS DISTINCT FROM NEW.status THEN
        INSERT INTO order_audit(order_id, old_status, new_status)
        VALUES (NEW.id, OLD.status, NEW.status);
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_orders_audit
    AFTER UPDATE ON orders
    FOR EACH ROW EXECUTE FUNCTION audit_order_status_change();

-- Ch05-07: Stored procedure — batch replenish stock
CREATE OR REPLACE PROCEDURE replenish_stock(p_product_id BIGINT, p_quantity INT)
LANGUAGE plpgsql AS $$
BEGIN
    UPDATE products
       SET stock_quantity = stock_quantity + p_quantity,
           updated_at     = NOW()
     WHERE id = p_product_id;
    IF NOT FOUND THEN
        RAISE EXCEPTION 'Product % not found', p_product_id;
    END IF;
END;
$$;

-- Ch05-07: Function — calculate order total
CREATE OR REPLACE FUNCTION order_total(p_order_id BIGINT)
RETURNS NUMERIC(12,2) AS $$
DECLARE
    v_total NUMERIC(12,2);
BEGIN
    SELECT COALESCE(SUM(quantity * unit_price), 0)
      INTO v_total
      FROM order_items
     WHERE order_id = p_order_id;
    RETURN v_total;
END;
$$ LANGUAGE plpgsql STABLE;

-- Ch05-09: Replication slot (logical) — used by Debezium CDC
-- SELECT pg_create_logical_replication_slot('shopverse_cdc', 'pgoutput');

-- Seed data
INSERT INTO customers(first_name, last_name, email, tier)
VALUES ('Admin', 'User', 'admin@shopverse.com', 'PLATINUM');
