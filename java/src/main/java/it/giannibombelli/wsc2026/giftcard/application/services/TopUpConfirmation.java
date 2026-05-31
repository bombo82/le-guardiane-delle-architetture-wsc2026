package it.giannibombelli.wsc2026.giftcard.application.services;

import it.giannibombelli.wsc2026.giftcard.application.commands.ConfirmTopUp;
import it.giannibombelli.wsc2026.giftcard.application.usecases.TopUpConfirming;
import it.giannibombelli.wsc2026.giftcard.domain.policies.ConfirmTopUpPolicy;
import it.giannibombelli.wsc2026.payment.domain.events.PaymentResultEvents;

import static java.util.Objects.requireNonNull;


public class TopUpConfirmation {
    private final ConfirmTopUpPolicy policy;
    private final TopUpConfirming useCase;

    public TopUpConfirmation(ConfirmTopUpPolicy paymentPolicy, TopUpConfirming paymentConfirmation) {
        this.policy = requireNonNull(paymentPolicy);
        this.useCase = requireNonNull(paymentConfirmation);
    }

    public void handlePaymentResults(PaymentResultEvents event) {
        final ConfirmTopUp cmd = policy.evaluate(event);

        if (cmd != null) {
            useCase.invoke(cmd);
        }
    }
}
