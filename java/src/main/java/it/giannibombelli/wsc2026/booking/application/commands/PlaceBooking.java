package it.giannibombelli.wsc2026.booking.application.commands;

import it.giannibombelli.wsc2026.common.utils.Require;

import it.giannibombelli.wsc2026.booking.domain.booking.BookingId;
import it.giannibombelli.wsc2026.booking.domain.primitive.GiftCardReference;
import it.giannibombelli.wsc2026.common.application.Command;
import it.giannibombelli.wsc2026.common.domain.primitive.Description;
import it.giannibombelli.wsc2026.common.domain.primitive.Money;


public record PlaceBooking(BookingId aggregateId, Money amount, Description description,
                           GiftCardReference giftCardReference) implements Command<BookingId> {
    public PlaceBooking {
        Require.requireArgument(aggregateId, "bookingId");
        Require.requireArgument(amount, "amount");
        Require.requireArgument(description, "description");
        Require.requireArgument(giftCardReference, "giftCardReference");
    }
}
