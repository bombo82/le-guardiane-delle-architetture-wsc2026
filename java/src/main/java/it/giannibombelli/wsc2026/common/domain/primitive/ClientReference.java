package it.giannibombelli.wsc2026.common.domain.primitive;

import it.giannibombelli.wsc2026.common.domain.identity.EntityId;
import it.giannibombelli.wsc2026.common.utils.Require;

import java.util.UUID;

public record ClientReference(UUID value) implements EntityId {
    public ClientReference {
        Require.requireArgument(value, "clientReference");
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
