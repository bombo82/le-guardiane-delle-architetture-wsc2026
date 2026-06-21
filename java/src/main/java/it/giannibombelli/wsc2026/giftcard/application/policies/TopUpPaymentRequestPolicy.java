package it.giannibombelli.wsc2026.giftcard.application.policies;

import it.giannibombelli.wsc2026.common.utils.Require;

import it.giannibombelli.wsc2026.common.domain.identity.EntityId;
import it.giannibombelli.wsc2026.common.application.Policy;
import it.giannibombelli.wsc2026.common.domain.primitive.ClientReference;
import it.giannibombelli.wsc2026.common.domain.primitive.Timestamp;
import it.giannibombelli.wsc2026.giftcard.domain.events.GiftCardTopUpRequested;
import it.giannibombelli.wsc2026.payment.application.commands.RequestPayment;
import it.giannibombelli.wsc2026.payment.domain.payment.PaymentId;


public class TopUpPaymentRequestPolicy implements Policy<GiftCardTopUpRequested, RequestPayment> {

    @Override
    public RequestPayment evaluate(GiftCardTopUpRequested event) {
        Require.requireArgument(event, "Top-up request event");
        return new RequestPayment(
            EntityId.generate(PaymentId::new),
            new ClientReference(event.aggregateId().value()),
            event.requestedAmount(),
            Timestamp.now()
        );
    }
}
