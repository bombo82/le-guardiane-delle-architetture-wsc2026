package it.giannibombelli.wsc2026.giftcard.application.services;

import it.giannibombelli.wsc2026.booking.domain.events.BookingResultEvents;
import it.giannibombelli.wsc2026.giftcard.application.commands.CreditGiftCard;
import it.giannibombelli.wsc2026.giftcard.application.usecases.GiftCardCrediting;
import it.giannibombelli.wsc2026.giftcard.application.policies.CreditGiftCardPolicy;

import static java.util.Objects.requireNonNull;


public class BookingResultCrediting {
    private final CreditGiftCardPolicy policy;
    private final GiftCardCrediting useCase;

    public BookingResultCrediting(CreditGiftCardPolicy creditGiftCardPolicy, GiftCardCrediting giftCardCrediting) {
        this.policy = requireNonNull(creditGiftCardPolicy);
        this.useCase = requireNonNull(giftCardCrediting);
    }

    public void handleBookingResults(BookingResultEvents events) {
        final CreditGiftCard cmd = policy.evaluate(events);

        if (cmd != null) {
            useCase.invoke(cmd);
        }
    }
}
