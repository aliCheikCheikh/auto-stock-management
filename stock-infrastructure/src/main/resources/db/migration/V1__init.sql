-- Initial schema for auto-stock-management
CREATE TABLE category (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    CHECK (length(trim(name)) > 0)
);

CREATE TABLE shop (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    address VARCHAR(500) NOT NULL,
    CHECK (length(trim(name)) > 0),
    CHECK (length(trim(address)) > 0)
);

CREATE TABLE app_user (
    id UUID PRIMARY KEY,
    username VARCHAR(255) NOT NULL UNIQUE,
    role VARCHAR(50) NOT NULL,
    CHECK (length(trim(username)) > 0),
    CHECK (role IN ('OWNER', 'SELLER'))
);

CREATE TABLE product (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    reference VARCHAR(100) NOT NULL UNIQUE,
    category_id UUID NOT NULL,
    minimum_global_threshold INTEGER NOT NULL,
    unit_price_amount NUMERIC(15, 2) NOT NULL,
    unit_price_currency CHAR(3) NOT NULL,
    CONSTRAINT fk_product_category
        FOREIGN KEY (category_id)
        REFERENCES category(id),
    CHECK (length(trim(name)) > 0),
    CHECK (length(trim(reference)) > 0),
    CHECK (minimum_global_threshold >= 0),
    CHECK (unit_price_amount > 0),
    CHECK (length(trim(unit_price_currency)) = 3)
);

CREATE INDEX idx_product_category_id ON product(category_id);

CREATE TABLE storage_location (
    id UUID PRIMARY KEY,
    shop_id UUID NOT NULL,
    location_type VARCHAR(50) NOT NULL,
    label VARCHAR(255) NOT NULL,
    low_stock_indicator INTEGER NOT NULL,
    CONSTRAINT fk_storage_location_shop
        FOREIGN KEY (shop_id)
        REFERENCES shop(id),
    CHECK (location_type IN ('SHOP_FLOOR', 'BACKSTOCK')),
    CHECK (length(trim(label)) > 0),
    CHECK (low_stock_indicator >= 0)
);

CREATE INDEX idx_storage_location_shop_id ON storage_location(shop_id);

CREATE TABLE stock_level (
    location_id UUID NOT NULL,
    product_id UUID NOT NULL,
    quantity INTEGER NOT NULL,
    PRIMARY KEY (location_id, product_id),
    CONSTRAINT fk_stock_level_location
        FOREIGN KEY (location_id)
        REFERENCES storage_location(id),
    CONSTRAINT fk_stock_level_product
        FOREIGN KEY (product_id)
        REFERENCES product(id),
    CHECK (quantity >= 0)
);

CREATE INDEX idx_stock_level_product_id ON stock_level(product_id);

CREATE TABLE sale (
    id UUID PRIMARY KEY,
    sold_by UUID NOT NULL,
    occurred_at TIMESTAMP NOT NULL,
    total_amount NUMERIC(15, 2) NOT NULL,
    total_currency CHAR(3) NOT NULL,
    CONSTRAINT fk_sale_sold_by
        FOREIGN KEY (sold_by)
        REFERENCES app_user(id),
    CHECK (total_amount > 0),
    CHECK (length(trim(total_currency)) = 3)
);

CREATE INDEX idx_sale_sold_by ON sale(sold_by);
CREATE INDEX idx_sale_occurred_at ON sale(occurred_at);

CREATE TABLE sale_line (
    sale_id UUID NOT NULL,
    line_number INTEGER NOT NULL,
    product_id UUID NOT NULL,
    quantity INTEGER NOT NULL,
    unit_price_amount NUMERIC(15, 2) NOT NULL,
    unit_price_currency CHAR(3) NOT NULL,
    line_total_amount NUMERIC(15, 2) NOT NULL,
    line_total_currency CHAR(3) NOT NULL,
    PRIMARY KEY (sale_id, line_number),
    CONSTRAINT fk_sale_line_sale
        FOREIGN KEY (sale_id)
        REFERENCES sale(id),
    CONSTRAINT fk_sale_line_product
        FOREIGN KEY (product_id)
        REFERENCES product(id),
    CHECK (line_number > 0),
    CHECK (quantity > 0),
    CHECK (unit_price_amount > 0),
    CHECK (line_total_amount > 0),
    CHECK (unit_price_currency = line_total_currency),
    CHECK (length(trim(unit_price_currency)) = 3),
    CHECK (length(trim(line_total_currency)) = 3)
);

CREATE INDEX idx_sale_line_product_id ON sale_line(product_id);

CREATE TABLE stock_movement (
    id UUID PRIMARY KEY,
    product_id UUID NOT NULL,
    source_location_id UUID,
    destination_location_id UUID,
    movement_type VARCHAR(50) NOT NULL,
    quantity INTEGER NOT NULL,
    performed_by UUID NOT NULL,
    occurred_at TIMESTAMP NOT NULL,
    sale_id UUID,
    CONSTRAINT fk_stock_movement_product
        FOREIGN KEY (product_id)
        REFERENCES product(id),
    CONSTRAINT fk_stock_movement_source_location
        FOREIGN KEY (source_location_id)
        REFERENCES storage_location(id),
    CONSTRAINT fk_stock_movement_destination_location
        FOREIGN KEY (destination_location_id)
        REFERENCES storage_location(id),
    CONSTRAINT fk_stock_movement_performed_by
        FOREIGN KEY (performed_by)
        REFERENCES app_user(id),
    CONSTRAINT fk_stock_movement_sale
        FOREIGN KEY (sale_id)
        REFERENCES sale(id),
    CHECK (movement_type IN ('ENTRY', 'EXIT', 'TRANSFER')),
    CHECK (quantity > 0),
    CHECK (
        (movement_type = 'ENTRY'
            AND source_location_id IS NULL
            AND destination_location_id IS NOT NULL
            AND sale_id IS NULL)
        OR
        (movement_type = 'EXIT'
            AND source_location_id IS NOT NULL
            AND destination_location_id IS NULL
            AND sale_id IS NOT NULL)
        OR
        (movement_type = 'TRANSFER'
            AND source_location_id IS NOT NULL
            AND destination_location_id IS NOT NULL
            AND source_location_id <> destination_location_id
            AND sale_id IS NULL)
    )
);

CREATE INDEX idx_stock_movement_source_location_id ON stock_movement(source_location_id);
CREATE INDEX idx_stock_movement_destination_location_id ON stock_movement(destination_location_id);
CREATE INDEX idx_stock_movement_performed_by ON stock_movement(performed_by);
CREATE INDEX idx_stock_movement_occurred_at ON stock_movement(occurred_at);
CREATE INDEX idx_stock_movement_product_occurred_at ON stock_movement(product_id, occurred_at);
CREATE INDEX idx_stock_movement_sale_occurred_at ON stock_movement(sale_id, occurred_at);

