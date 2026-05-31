-- V1: Initial schema for GiftCard Bounded Context
-- Consolidated single migration for the GiftCard aggregate.

CREATE TABLE gift_card (
    id      TEXT PRIMARY KEY NOT NULL,   -- UUID stored as canonical text form
    balance DECIMAL(19,2) NOT NULL       -- monetary amounts with 2 decimals
);
