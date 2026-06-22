package it.giannibombelli.wsc2026.giftcard.application.integration.payment.handlers;

import it.giannibombelli.wsc2026.giftcard.application.commands.ConfirmTopUp;
import it.giannibombelli.wsc2026.giftcard.application.integration.payment.adapter.PaymentResult;
import it.giannibombelli.wsc2026.giftcard.application.usecases.TopUpConfirming;
import it.giannibombelli.wsc2026.payment.integration.PaymentResultIntegrationEvent;

import static java.util.Objects.requireNonNull;

public final class ConfirmTopUpFromPayment {
    private final PaymentResult paymentResult;
    private final TopUpConfirming useCase;

    public ConfirmTopUpFromPayment(PaymentResult paymentResult, TopUpConfirming topUpConfirming) {
        this.paymentResult = requireNonNull(paymentResult);
        this.useCase = requireNonNull(topUpConfirming);
    }

    public void handle(PaymentResultIntegrationEvent event) {
        final ConfirmTopUp cmd = paymentResult.adapt(event);

        if (cmd != null) {
            useCase.invoke(cmd);
        }
    }
}
