package it.giannibombelli.wsc2026.payment.application.commands;

import it.giannibombelli.wsc2026.common.utils.Require;

import it.giannibombelli.wsc2026.common.application.Command;
import it.giannibombelli.wsc2026.common.domain.primitive.Money;
import it.giannibombelli.wsc2026.common.domain.primitive.Timestamp;
import it.giannibombelli.wsc2026.payment.domain.payment.PaymentId;
import it.giannibombelli.wsc2026.payment.domain.payment.Provider;
import it.giannibombelli.wsc2026.payment.domain.payment.ProviderReference;

public record StartTransaction(PaymentId aggregateId, Provider provider, ProviderReference providerReference, Money amount,
                               Timestamp startedAt)
    implements Command<PaymentId> {
    public StartTransaction {
        Require.requireArgument(aggregateId, "aggregateId");
        Require.requireArgument(provider, "provider");
        Require.requireArgument(amount, "amount");
        Require.requireArgument(startedAt, "startedAt");
    }
}
