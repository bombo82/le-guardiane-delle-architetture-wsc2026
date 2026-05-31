package it.giannibombelli.wsc2026.payment.domain.payment;

import it.giannibombelli.wsc2026.common.utils.Require;

import it.giannibombelli.wsc2026.common.domain.identity.EntityId;

import java.util.Objects;
import java.util.UUID;

public record TransactionId(UUID value) implements EntityId {
    public TransactionId {
        Require.requireArgument(value, "value");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        return Objects.equals(value, ((TransactionId) o).value);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(value);
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
