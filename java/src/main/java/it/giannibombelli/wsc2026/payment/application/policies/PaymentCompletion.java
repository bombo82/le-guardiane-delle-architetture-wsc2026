package it.giannibombelli.wsc2026.payment.application.policies;

import it.giannibombelli.wsc2026.common.utils.Require;

import it.giannibombelli.wsc2026.common.application.Policy;
import it.giannibombelli.wsc2026.payment.application.commands.AcceptTransaction;
import it.giannibombelli.wsc2026.payment.domain.events.TransactionAccepted;

public final class PaymentCompletion implements Policy<TransactionAccepted, AcceptTransaction> {

    @Override
    public AcceptTransaction evaluate(TransactionAccepted event) {
        Require.requireArgument(event, "event");
        return new AcceptTransaction(
            event.aggregateId(),
            event.transactionId(),
            event.providerCompletedAt()
        );
    }
}
