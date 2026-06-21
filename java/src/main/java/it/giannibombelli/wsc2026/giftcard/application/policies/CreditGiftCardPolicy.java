package it.giannibombelli.wsc2026.giftcard.application.policies;

import it.giannibombelli.wsc2026.booking.domain.events.BookingResultEvents;
import it.giannibombelli.wsc2026.common.application.Policy;
import it.giannibombelli.wsc2026.common.utils.Require;
import it.giannibombelli.wsc2026.giftcard.application.commands.CreditGiftCard;
import it.giannibombelli.wsc2026.giftcard.domain.giftcard.GiftCardId;


public final class CreditGiftCardPolicy implements Policy<BookingResultEvents, CreditGiftCard> {
    @Override
    public CreditGiftCard evaluate(BookingResultEvents event) {
        Require.requireArgument(event, "Booking event");
        return switch (event) {
            case BookingResultEvents.BookingConfirmed it -> new CreditGiftCard(new GiftCardId(it.giftCardReference()), it.amount());
            case BookingResultEvents.BookingRefused it -> new CreditGiftCard(new GiftCardId(it.giftCardReference()), it.amount());
            case BookingResultEvents.BookingRejected ignored -> null;
        };
    }
}
