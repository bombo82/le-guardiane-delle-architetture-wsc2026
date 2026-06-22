package it.giannibombelli.wsc2026.giftcard.application.integration.booking.handlers;

import it.giannibombelli.wsc2026.booking.integration.giftcard.BookingResultIntegrationEvent;
import it.giannibombelli.wsc2026.giftcard.application.commands.RefundGiftCard;
import it.giannibombelli.wsc2026.giftcard.application.integration.booking.adapter.BookingResult;
import it.giannibombelli.wsc2026.giftcard.application.usecases.GiftCardRefunding;

import static java.util.Objects.requireNonNull;

public final class RefundFromBooking {
    private final BookingResult bookingResult;
    private final GiftCardRefunding useCase;

    public RefundFromBooking(BookingResult bookingResult, GiftCardRefunding giftCardRefunding) {
        this.bookingResult = requireNonNull(bookingResult);
        this.useCase = requireNonNull(giftCardRefunding);
    }

    public void handle(BookingResultIntegrationEvent.BookingRejectedIntegrationEvent event) {
        final RefundGiftCard cmd = bookingResult.adaptRejected(event);

        if (cmd != null) {
            useCase.invoke(cmd);
        }
    }
}
