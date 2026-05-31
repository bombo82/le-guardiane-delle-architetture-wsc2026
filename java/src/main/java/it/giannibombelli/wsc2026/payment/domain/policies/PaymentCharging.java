package it.giannibombelli.wsc2026.payment.domain.policies;

import it.giannibombelli.wsc2026.common.utils.Require;

import it.giannibombelli.wsc2026.payment.domain.events.TransactionStarted;
import it.giannibombelli.wsc2026.payment.domain.ports.PaymentProvider;
import it.giannibombelli.wsc2026.payment.domain.ports.PaymentProviderResult;

import static java.util.Objects.requireNonNull;

public final class PaymentCharging {

    private final PaymentProvider provider;

    public PaymentCharging(PaymentProvider provider) {
        this.provider = requireNonNull(provider);
    }

    public PaymentProviderResult charge(TransactionStarted event) {
        Require.requireArgument(event, "event");
        return provider.process(
            event.aggregateId().value(),
            event.transactionId().value(),
            event.amount()
        );
    }
}
