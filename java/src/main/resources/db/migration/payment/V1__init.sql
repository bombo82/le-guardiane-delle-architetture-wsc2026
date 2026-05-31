-- V1: Initial schema for Payment Bounded Context
-- Consolidated single migration for the Payment aggregate and its transactions.

CREATE TABLE payment (
    id                  TEXT PRIMARY KEY NOT NULL,   -- UUID stored as canonical text form
    client_reference    TEXT NOT NULL,               -- opaque reference provided by the caller
    amount              DECIMAL(19,2) NOT NULL,      -- monetary amount with 2 decimals
    status              TEXT NOT NULL,               -- PaymentStatus enum value
    requested_at        TEXT                         -- Instant when the payment was requested
);

CREATE TABLE payment_transaction (
    id                     INTEGER PRIMARY KEY AUTOINCREMENT,
    payment_id             TEXT NOT NULL,
    provider               TEXT NOT NULL,
    provider_reference     TEXT,                        -- nullable for rejections without a reference
    amount                 DECIMAL(19,2) NOT NULL,      -- monetary amount of this transaction decision
    provider_completed_at  TEXT,                        -- Instant when provider completed the transaction (nullable for rejections)
    accepted               INTEGER NOT NULL DEFAULT 0,  -- 1 = accepted, 0 = rejected
    transaction_id         TEXT,                        -- stable transaction identifier
    started_at             TEXT,                        -- Instant when the transaction started
    status                 TEXT NOT NULL DEFAULT 'STARTED'
);

CREATE INDEX idx_payment_transaction_payment_id ON payment_transaction(payment_id);
