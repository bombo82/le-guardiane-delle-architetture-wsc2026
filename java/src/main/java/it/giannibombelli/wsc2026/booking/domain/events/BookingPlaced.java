package it.giannibombelli.wsc2026.booking.domain.events;

import it.giannibombelli.wsc2026.common.utils.Require;

import it.giannibombelli.wsc2026.booking.domain.booking.BookingId;
import it.giannibombelli.wsc2026.common.domain.primitive.Description;
import it.giannibombelli.wsc2026.common.domain.primitive.Money;
import it.giannibombelli.wsc2026.giftcard.domain.giftcard.GiftCardId;


public record BookingPlaced(BookingId aggregateId, Money amount, Description description,
                            GiftCardId giftCardId) implements BookingEvent {
    public BookingPlaced {
        Require.requireArgument(aggregateId, "bookingId");
        Require.requireArgument(amount, "amount");
        Require.requireArgument(description, "description");
        Require.requireArgument(giftCardId, "giftCardId");
    }
}
