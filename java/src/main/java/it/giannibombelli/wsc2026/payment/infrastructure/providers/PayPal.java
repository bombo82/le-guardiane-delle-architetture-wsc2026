package it.giannibombelli.wsc2026.payment.infrastructure.providers;

import it.giannibombelli.wsc2026.common.domain.primitive.Money;
import it.giannibombelli.wsc2026.common.domain.primitive.Timestamp;
import it.giannibombelli.wsc2026.payment.domain.ports.PaymentProvider;
import it.giannibombelli.wsc2026.payment.domain.ports.PaymentProviderResult;

import java.util.UUID;

public final class PayPal implements PaymentProvider {
    @Override
    public PaymentProviderResult process(UUID paymentId, UUID providerReference, Money amount) {
        return new PaymentProviderResult.Success(UUID.randomUUID(), Timestamp.now());
    }

    @Override
    public PaymentProviderResult refund(UUID paymentId, UUID providerReference, Money amount) {
        return new PaymentProviderResult.Success(UUID.randomUUID(), Timestamp.now());
    }
}
