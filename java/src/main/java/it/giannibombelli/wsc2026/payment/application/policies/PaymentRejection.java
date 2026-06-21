package it.giannibombelli.wsc2026.payment.application.policies;

import it.giannibombelli.wsc2026.common.utils.Require;

import it.giannibombelli.wsc2026.common.application.Policy;
import it.giannibombelli.wsc2026.payment.application.commands.RejectTransaction;
import it.giannibombelli.wsc2026.payment.domain.events.TransactionRejected;

public final class PaymentRejection implements Policy<TransactionRejected, RejectTransaction> {

    @Override
    public RejectTransaction evaluate(TransactionRejected event) {
        Require.requireArgument(event, "event");
        return new RejectTransaction(
            event.aggregateId(),
            event.transactionId(),
            event.reason()
        );
    }
}
