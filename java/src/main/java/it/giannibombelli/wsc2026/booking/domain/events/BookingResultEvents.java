package it.giannibombelli.wsc2026.booking.domain.events;

import it.giannibombelli.wsc2026.common.utils.Require;

import it.giannibombelli.wsc2026.booking.domain.booking.BookingId;
import it.giannibombelli.wsc2026.common.domain.primitive.Money;
import it.giannibombelli.wsc2026.giftcard.domain.giftcard.GiftCardId;


public sealed interface BookingResultEvents extends BookingEvent
    permits BookingResultEvents.BookingConfirmed, BookingResultEvents.BookingRefused, BookingResultEvents.BookingRejected {

    record BookingConfirmed(
        BookingId aggregateId,
        GiftCardId giftCardId,
        Money amount
    ) implements BookingResultEvents {
        public BookingConfirmed {
            Require.requireArgument(aggregateId, "bookingId");
            Require.requireArgument(giftCardId, "giftCardId");
            Require.requireArgument(amount, "amount");
        }
    }

    record BookingRefused(
        BookingId aggregateId,
        GiftCardId giftCardId,
        Money amount
    ) implements BookingResultEvents {
        public BookingRefused {
            Require.requireArgument(aggregateId, "bookingId");
            Require.requireArgument(giftCardId, "giftCardId");
            Require.requireArgument(amount, "amount");
        }
    }

    record BookingRejected(
        BookingId aggregateId,
        GiftCardId giftCardId,
        Money amount
    ) implements BookingResultEvents {
        public BookingRejected {
            Require.requireArgument(aggregateId, "bookingId");
            Require.requireArgument(giftCardId, "giftCardId");
            Require.requireArgument(amount, "amount");
        }
    }
}
