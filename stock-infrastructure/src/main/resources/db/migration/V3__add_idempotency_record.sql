CREATE TABLE idempotency_record(
  idempotency_key UUID PRIMARY KEY NOT NULL,
  request_method VARCHAR(10) NOT NULL,
  request_path VARCHAR(255) NOT NULL,
  request_hash CHAR(64) NOT NULL,
  response_status INTEGER NOT NULL,
  response_body TEXT NOT NULL,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL,
  CHECK(response_status BETWEEN 100 AND 599)

);

CREATE INDEX idx_idempotency_key_created_at ON idempotency_record(created_at);