package it.giannibombelli.wsc2026.common.domain.model;

import it.giannibombelli.wsc2026.common.domain.identity.EntityId;

public interface Entity<ID extends EntityId> {
    ID id();
}
