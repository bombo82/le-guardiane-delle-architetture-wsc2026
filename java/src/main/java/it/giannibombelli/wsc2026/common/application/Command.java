package it.giannibombelli.wsc2026.common.application;

import it.giannibombelli.wsc2026.common.domain.identity.AggregateId;

public interface Command<ID extends AggregateId> {
    ID aggregateId();
}
