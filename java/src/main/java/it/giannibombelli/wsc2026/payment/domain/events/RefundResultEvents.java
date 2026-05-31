package it.giannibombelli.wsc2026.payment.domain.events;

import it.giannibombelli.wsc2026.common.utils.Require;

import it.giannibombelli.wsc2026.common.domain.primitive.ClientReference;
import it.giannibombelli.wsc2026.common.domain.primitive.Description;
import it.giannibombelli.wsc2026.common.domain.primitive.Money;
import it.giannibombelli.wsc2026.payment.domain.payment.PaymentId;

public sealed interface RefundResultEvents extends PaymentEvent
    permits RefundResultEvents.TransactionRefunded, RefundResultEvents.TransactionNotRefunded {

    record TransactionRefunded(
        PaymentId aggregateId,
        ClientReference clientReference,
        Money amount
    ) implements RefundResultEvents {
        public TransactionRefunded {
            Require.requireArgument(aggregateId, "paymentId");
            Require.requireArgument(clientReference, "clientReference");
            Require.requireArgument(amount, "amount");
        }
    }

    record TransactionNotRefunded(
        PaymentId aggregateId,
        ClientReference clientReference,
        Description reason
    ) implements RefundResultEvents {
        public TransactionNotRefunded {
            Require.requireArgument(aggregateId, "paymentId");
            Require.requireArgument(clientReference, "clientReference");
            Require.requireArgument(reason, "reason");
        }
    }
}
