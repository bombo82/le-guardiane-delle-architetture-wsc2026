package it.giannibombelli.wsc2026.booking.application.commands;

import it.giannibombelli.wsc2026.common.utils.Require;

import it.giannibombelli.wsc2026.booking.domain.booking.BookingId;
import it.giannibombelli.wsc2026.booking.domain.primitive.GiftCardReference;
import it.giannibombelli.wsc2026.common.application.Command;
import it.giannibombelli.wsc2026.common.domain.primitive.Money;


public sealed interface BookingConfirmationCommands extends Command<BookingId>
    permits BookingConfirmationCommands.ConfirmBooking, BookingConfirmationCommands.RejectBooking {

    record ConfirmBooking(
        BookingId aggregateId,
        GiftCardReference giftCardReference,
        Money amount
    ) implements BookingConfirmationCommands {
        public ConfirmBooking {
            Require.requireArgument(aggregateId, "bookingId");
            Require.requireArgument(giftCardReference, "giftCardReference");
            Require.requireArgument(amount, "amount");
        }
    }

    record RejectBooking(
        BookingId aggregateId,
        GiftCardReference giftCardReference,
        Money amount
    ) implements BookingConfirmationCommands {
        public RejectBooking {
            Require.requireArgument(aggregateId, "bookingId");
            Require.requireArgument(giftCardReference, "giftCardReference");
            Require.requireArgument(amount, "amount");
        }
    }
}
