-- V1: Initial schema for Booking Bounded Context
-- Consolidated single migration for the Booking aggregate.

CREATE TABLE booking (
    id           TEXT PRIMARY KEY NOT NULL,      -- UUID stored as canonical text form
    description  TEXT NOT NULL,                  -- booking description
    gift_card_id TEXT NOT NULL,                  -- associated gift card UUID
    status       TEXT NOT NULL DEFAULT 'PLACED'  -- lifecycle status
);
