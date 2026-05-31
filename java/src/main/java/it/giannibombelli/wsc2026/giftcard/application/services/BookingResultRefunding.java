package it.giannibombelli.wsc2026.giftcard.application.services;

import it.giannibombelli.wsc2026.booking.domain.events.BookingResultEvents;
import it.giannibombelli.wsc2026.giftcard.application.commands.RefundGiftCard;
import it.giannibombelli.wsc2026.giftcard.application.usecases.GiftCardRefunding;
import it.giannibombelli.wsc2026.giftcard.domain.policies.RefundGiftCardPolicy;

import static java.util.Objects.requireNonNull;


public class BookingResultRefunding {
    private final RefundGiftCardPolicy policy;
    private final GiftCardRefunding useCase;

    public BookingResultRefunding(RefundGiftCardPolicy refundGiftCardPolicy, GiftCardRefunding giftCardRefunding) {
        this.policy = requireNonNull(refundGiftCardPolicy);
        this.useCase = requireNonNull(giftCardRefunding);
    }

    public void handleBookingResults(BookingResultEvents events) {
        final RefundGiftCard cmd = policy.evaluate(events);

        if (cmd != null) {
            useCase.invoke(cmd);
        }
    }
}
