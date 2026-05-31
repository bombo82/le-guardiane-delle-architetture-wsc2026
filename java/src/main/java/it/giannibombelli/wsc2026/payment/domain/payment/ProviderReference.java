package it.giannibombelli.wsc2026.payment.domain.payment;

import it.giannibombelli.wsc2026.common.utils.Require;

import java.util.UUID;

public record ProviderReference(UUID value) {
    public ProviderReference {
        Require.requireArgument(value, "providerReference");
    }
}
