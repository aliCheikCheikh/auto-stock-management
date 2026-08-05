-- Journal durable d'un import de réception.
--
-- L'identifiant est fourni par le client avant confirmation. L'empreinte lie cet identifiant au
-- fichier, à la boutique, à l'utilisateur et aux lignes choisies. Une relance identique peut ainsi
-- reprendre sans doubler le stock, tandis qu'une réutilisation ambiguë est refusée.
CREATE TABLE stock_receipt_import_execution (
    import_id UUID PRIMARY KEY,
    fingerprint CHAR(64) NOT NULL,
    shop_id UUID NOT NULL,
    user_id UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_stock_receipt_import_shop
        FOREIGN KEY (shop_id) REFERENCES shop(id),
    CONSTRAINT fk_stock_receipt_import_user
        FOREIGN KEY (user_id) REFERENCES app_user(id),
    CONSTRAINT chk_stock_receipt_import_fingerprint
        CHECK (fingerprint ~ '^[0-9a-f]{64}$')
);

-- Seuls les résultats qui ont nécessité une tentative d'écriture sont persistés. Les lignes
-- invalides ou décochées restent reproductibles depuis le fichier, sans encombrer le journal.
CREATE TABLE stock_receipt_import_row_result (
    id UUID PRIMARY KEY,
    import_id UUID NOT NULL,
    line_number INTEGER NOT NULL,
    status VARCHAR(32) NOT NULL,
    action VARCHAR(32) NOT NULL,
    product_reference VARCHAR(100) NOT NULL,
    product_name VARCHAR(255) NOT NULL,
    product_id UUID,
    quantity_received INTEGER NOT NULL,
    issue_field VARCHAR(50),
    issue_code VARCHAR(50),
    issue_message VARCHAR(500),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_stock_receipt_import_row_execution
        FOREIGN KEY (import_id) REFERENCES stock_receipt_import_execution(import_id) ON DELETE CASCADE,
    CONSTRAINT fk_stock_receipt_import_row_product
        FOREIGN KEY (product_id) REFERENCES product(id),
    CONSTRAINT uq_stock_receipt_import_row UNIQUE (import_id, line_number),
    CONSTRAINT chk_stock_receipt_import_row_line CHECK (line_number >= 2),
    CONSTRAINT chk_stock_receipt_import_row_action
        CHECK (action IN ('CREATE_PRODUCT', 'RECEIVE_EXISTING')),
    CONSTRAINT chk_stock_receipt_import_row_result CHECK (
        (
            status = 'IMPORTED'
            AND product_id IS NOT NULL
            AND quantity_received > 0
            AND issue_field IS NULL
            AND issue_code IS NULL
            AND issue_message IS NULL
        )
        OR
        (
            status = 'FAILED'
            AND product_id IS NULL
            AND quantity_received = 0
            AND issue_field IS NOT NULL
            AND issue_code IS NOT NULL
            AND issue_message IS NOT NULL
        )
    )
);

CREATE INDEX idx_stock_receipt_import_created_at
    ON stock_receipt_import_execution(created_at);
