package it.giannibombelli.wsc2026.common.domain.model;

import it.giannibombelli.wsc2026.common.domain.identity.AggregateId;

public interface Aggregate<ID extends AggregateId> extends Entity<ID> {
}
