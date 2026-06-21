package it.giannibombelli.wsc2026.giftcard.domain.policies;

import it.giannibombelli.wsc2026.common.domain.model.Policy;
import it.giannibombelli.wsc2026.common.utils.Require;
import it.giannibombelli.wsc2026.giftcard.application.commands.ConfirmTopUp;
import it.giannibombelli.wsc2026.giftcard.domain.giftcard.GiftCardId;
import it.giannibombelli.wsc2026.payment.domain.events.PaymentResultEvents;

import java.util.UUID;

import static it.giannibombelli.wsc2026.payment.domain.events.PaymentResultEvents.*;


public final class ConfirmTopUpPolicy implements Policy<PaymentResultEvents, ConfirmTopUp> {
    @Override
    public ConfirmTopUp evaluate(PaymentResultEvents event) {
        Require.requireArgument(event, "Payment event");
        return switch (event) {
            case PaymentAccepted it ->
                new ConfirmTopUp(new GiftCardId(it.clientReference().value()), it.amount());
            case PaymentRejected ignored -> null;
            case PaymentExpired ignored -> null;
        };
    }
}
