package it.giannibombelli.wsc2026.booking.domain.events;

import it.giannibombelli.wsc2026.common.utils.Require;

import it.giannibombelli.wsc2026.booking.domain.booking.BookingId;
import it.giannibombelli.wsc2026.common.domain.primitive.Money;

import java.util.UUID;


public sealed interface BookingResultEvents extends BookingEvent
    permits BookingResultEvents.BookingConfirmed, BookingResultEvents.BookingRefused, BookingResultEvents.BookingRejected {

    record BookingConfirmed(
        BookingId aggregateId,
        UUID giftCardReference,
        Money amount
    ) implements BookingResultEvents {
        public BookingConfirmed {
            Require.requireArgument(aggregateId, "bookingId");
            Require.requireArgument(giftCardReference, "giftCardReference");
            Require.requireArgument(amount, "amount");
        }
    }

    record BookingRefused(
        BookingId aggregateId,
        UUID giftCardReference,
        Money amount
    ) implements BookingResultEvents {
        public BookingRefused {
            Require.requireArgument(aggregateId, "bookingId");
            Require.requireArgument(giftCardReference, "giftCardReference");
            Require.requireArgument(amount, "amount");
        }
    }

    record BookingRejected(
        BookingId aggregateId,
        UUID giftCardReference,
        Money amount
    ) implements BookingResultEvents {
        public BookingRejected {
            Require.requireArgument(aggregateId, "bookingId");
            Require.requireArgument(giftCardReference, "giftCardReference");
            Require.requireArgument(amount, "amount");
        }
    }
}
