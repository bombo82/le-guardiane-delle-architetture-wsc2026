package it.giannibombelli.wsc2026.giftcard.application.policies;

import it.giannibombelli.wsc2026.booking.domain.events.BookingResultEvents;
import it.giannibombelli.wsc2026.common.application.Policy;
import it.giannibombelli.wsc2026.common.utils.Require;
import it.giannibombelli.wsc2026.giftcard.application.commands.RefundGiftCard;


public final class RefundGiftCardPolicy implements Policy<BookingResultEvents, RefundGiftCard> {
    @Override
    public RefundGiftCard evaluate(BookingResultEvents event) {
        Require.requireArgument(event, "Booking event");
        return switch (event) {
            case BookingResultEvents.BookingRejected it -> new RefundGiftCard(it.giftCardId(), it.amount());
            case BookingResultEvents.BookingConfirmed ignored -> null;
            case BookingResultEvents.BookingRefused ignored -> null;
        };
    }
}
