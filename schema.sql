CREATE TABLE IF NOT EXISTS orders (
    id            SERIAL PRIMARY KEY,
    customer_name VARCHAR(100)  NOT NULL,
    product_name  VARCHAR(100)  NOT NULL,
    status        VARCHAR(20)   NOT NULL DEFAULT 'pending'
                  CHECK (status IN ('pending', 'shipped', 'delivered')),
    updated_at    TIMESTAMP     NOT NULL DEFAULT NOW()
);

CREATE OR REPLACE FUNCTION notify_orders_change()
RETURNS TRIGGER AS $$
DECLARE
    payload JSON;
BEGIN
    IF (TG_OP = 'DELETE') THEN
        payload = json_build_object(
            'operation', TG_OP,
            'table',     TG_TABLE_NAME,
            'data',      row_to_json(OLD)
        );
    ELSE
        NEW.updated_at = NOW();
        payload = json_build_object(
            'operation', TG_OP,
            'table',     TG_TABLE_NAME,
            'data',      row_to_json(NEW)
        );
    END IF;

    PERFORM pg_notify('orders_channel', payload::TEXT);

    IF (TG_OP = 'DELETE') THEN
        RETURN OLD;
    ELSE
        RETURN NEW;
    END IF;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS orders_change_trigger ON orders;

CREATE TRIGGER orders_change_trigger
    AFTER INSERT OR UPDATE OR DELETE
    ON orders
    FOR EACH ROW
    EXECUTE FUNCTION notify_orders_change();

INSERT INTO orders (customer_name, product_name, status) VALUES
    ('Raghav Zanwar',  'Laptop',    'pending'),
    ('XYX Zanwar',      'Mouse',   'shipped'),
    ('pankaj more', 'keyboard','delivered');