package it.giannibombelli.wsc2026.payment.application.policies;

import it.giannibombelli.wsc2026.common.utils.Require;

import it.giannibombelli.wsc2026.common.application.Policy;
import it.giannibombelli.wsc2026.common.domain.primitive.Timestamp;
import it.giannibombelli.wsc2026.payment.application.commands.ExpirePayment;
import it.giannibombelli.wsc2026.payment.domain.events.PaymentDeadlineReached;
import it.giannibombelli.wsc2026.payment.domain.payment.Payment;

public final class PaymentExpiration implements Policy<PaymentDeadlineReached, ExpirePayment> {

    private static final long DEADLINE_WINDOW_SECONDS = 48L * 3600;

    public boolean isDeadlineReached(Payment payment, Timestamp now) {
        Require.requireArgument(payment, "payment");
        Require.requireArgument(now, "now");

        Timestamp deadline = payment.requestedAt().plusSeconds(DEADLINE_WINDOW_SECONDS);
        return now.isAfterOrEqual(deadline);
    }

    @Override
    public ExpirePayment evaluate(PaymentDeadlineReached event) {
        Require.requireArgument(event, "event");
        return new ExpirePayment(event.aggregateId());
    }
}
