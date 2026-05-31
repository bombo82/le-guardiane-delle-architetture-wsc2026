package it.giannibombelli.wsc2026.payment.domain.events;

import it.giannibombelli.wsc2026.common.utils.Require;

import it.giannibombelli.wsc2026.common.domain.primitive.ClientReference;
import it.giannibombelli.wsc2026.common.domain.primitive.Description;
import it.giannibombelli.wsc2026.common.domain.primitive.Money;
import it.giannibombelli.wsc2026.payment.domain.payment.PaymentId;

public sealed interface PaymentResultEvents extends PaymentEvent
    permits PaymentResultEvents.PaymentAccepted, PaymentResultEvents.PaymentRejected, PaymentResultEvents.PaymentExpired {

    record PaymentAccepted(
        PaymentId aggregateId,
        ClientReference clientReference,
        Money amount
    ) implements PaymentResultEvents {
        public PaymentAccepted {
            Require.requireArgument(aggregateId, "paymentId");
            Require.requireArgument(clientReference, "clientReference");
            Require.requireArgument(amount, "amount");
        }
    }

    record PaymentRejected(PaymentId aggregateId, ClientReference clientReference, Money amount,
                           Description reason) implements PaymentResultEvents {
        public PaymentRejected {
            Require.requireArgument(aggregateId, "paymentId");
            Require.requireArgument(clientReference, "clientReference");
            Require.requireArgument(amount, "amount");
            Require.requireArgument(reason, "reason");
        }
    }

    record PaymentExpired(PaymentId aggregateId, ClientReference clientReference, Money amount) implements PaymentResultEvents {
        public PaymentExpired {
            Require.requireArgument(aggregateId, "paymentId");
            Require.requireArgument(clientReference, "clientReference");
            Require.requireArgument(amount, "amount");
        }
    }
}
