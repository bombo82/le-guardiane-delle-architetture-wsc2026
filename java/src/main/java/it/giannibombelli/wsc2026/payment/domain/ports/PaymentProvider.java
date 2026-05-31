package it.giannibombelli.wsc2026.payment.domain.ports;

import it.giannibombelli.wsc2026.common.domain.primitive.Money;

import java.util.UUID;

public interface PaymentProvider {
    PaymentProviderResult process(UUID paymentId, UUID providerReference, Money amount);

    PaymentProviderResult refund(UUID paymentId, UUID providerReference, Money amount);
}
