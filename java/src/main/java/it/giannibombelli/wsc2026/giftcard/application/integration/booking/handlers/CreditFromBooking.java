package it.giannibombelli.wsc2026.giftcard.application.integration.booking.handlers;

import it.giannibombelli.wsc2026.booking.integration.giftcard.BookingResultIntegrationEvent;
import it.giannibombelli.wsc2026.giftcard.application.commands.CreditGiftCard;
import it.giannibombelli.wsc2026.giftcard.application.integration.booking.adapter.BookingResult;
import it.giannibombelli.wsc2026.giftcard.application.usecases.GiftCardCrediting;

import static java.util.Objects.requireNonNull;

public final class CreditFromBooking {
    private final BookingResult bookingResult;
    private final GiftCardCrediting useCase;

    public CreditFromBooking(BookingResult bookingResult, GiftCardCrediting giftCardCrediting) {
        this.bookingResult = requireNonNull(bookingResult);
        this.useCase = requireNonNull(giftCardCrediting);
    }

    public void handle(BookingResultIntegrationEvent event) {
        final CreditGiftCard cmd = bookingResult.adapt(event);

        if (cmd != null) {
            useCase.invoke(cmd);
        }
    }
}
