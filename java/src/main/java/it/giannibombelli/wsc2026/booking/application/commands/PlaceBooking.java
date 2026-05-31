package it.giannibombelli.wsc2026.booking.application.commands;

import it.giannibombelli.wsc2026.common.utils.Require;

import it.giannibombelli.wsc2026.booking.domain.booking.BookingId;
import it.giannibombelli.wsc2026.common.application.Command;
import it.giannibombelli.wsc2026.common.domain.primitive.Description;
import it.giannibombelli.wsc2026.common.domain.primitive.Money;
import it.giannibombelli.wsc2026.giftcard.domain.giftcard.GiftCardId;


public record PlaceBooking(BookingId aggregateId, Money amount, Description description,
                           GiftCardId giftCardId) implements Command<BookingId> {
    public PlaceBooking {
        Require.requireArgument(aggregateId, "bookingId");
        Require.requireArgument(amount, "amount");
        Require.requireArgument(description, "description");
        Require.requireArgument(giftCardId, "giftCardId");
    }
}
