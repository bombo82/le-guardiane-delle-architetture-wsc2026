package it.giannibombelli.wsc2026.common.domain.model;

import it.giannibombelli.wsc2026.common.domain.identity.AggregateId;

public interface Event<ID extends AggregateId> {
    ID aggregateId();
}
