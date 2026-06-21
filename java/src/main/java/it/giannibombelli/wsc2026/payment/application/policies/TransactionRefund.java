package it.giannibombelli.wsc2026.payment.application.policies;

import it.giannibombelli.wsc2026.common.utils.Require;

import it.giannibombelli.wsc2026.common.application.Policy;
import it.giannibombelli.wsc2026.payment.application.commands.RefundTransaction;
import it.giannibombelli.wsc2026.payment.domain.events.RefundRequested;

public final class TransactionRefund implements Policy<RefundRequested, RefundTransaction> {

    @Override
    public RefundTransaction evaluate(RefundRequested event) {
        Require.requireArgument(event, "event");
        return new RefundTransaction(event.aggregateId(), event.amount());
    }
}
