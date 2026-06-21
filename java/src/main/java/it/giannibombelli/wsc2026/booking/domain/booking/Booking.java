package it.giannibombelli.wsc2026.booking.domain.booking;

import it.giannibombelli.wsc2026.common.utils.Require;

import it.giannibombelli.wsc2026.booking.domain.events.BookingResultEvents;
import it.giannibombelli.wsc2026.booking.domain.primitive.GiftCardReference;
import it.giannibombelli.wsc2026.common.domain.model.Aggregate;
import it.giannibombelli.wsc2026.common.domain.primitive.Description;
import it.giannibombelli.wsc2026.common.domain.primitive.Money;

import java.util.Objects;

import static it.giannibombelli.wsc2026.booking.domain.events.BookingResultEvents.*;


public final class Booking implements Aggregate<BookingId> {
    private final BookingId id;
    private final Description description;
    private final GiftCardReference giftCardReference;
    private BookingStatus status;

    public Booking(BookingId id, Description description, GiftCardReference giftCardReference, BookingStatus status) {
        Require.requireArgument(id, "id");
        Require.requireArgument(description, "description");
        Require.requireArgument(giftCardReference, "giftCardReference");
        Require.requireArgument(status, "status");

        this.id = id;
        this.description = description;
        this.giftCardReference = giftCardReference;
        this.status = status;
    }

    public static Booking place(BookingId id, Description description, GiftCardReference giftCardReference) {
        Require.requireArgument(id, "id");
        Require.requireArgument(description, "description");
        Require.requireArgument(giftCardReference, "giftCardReference");

        return new Booking(id, description, giftCardReference, BookingStatus.PLACED);
    }

    public BookingResultEvents confirm(Money amount) {
        Require.requireArgument(amount, "amount");

        // TODO: implement business rule for confirmation vs refusal
        // (e.g. check room availability, overbooking policy, etc.)
        boolean canConfirm = true;

        if (canConfirm) {
            status = BookingStatus.CONFIRMED;
            return new BookingConfirmed(this.id, this.giftCardReference.value(), amount);
        } else {
            status = BookingStatus.REFUSED;
            return new BookingRefused(this.id, this.giftCardReference.value(), amount);
        }
    }

    public BookingRejected reject(Money amount) {
        Require.requireArgument(amount, "amount");

        status = BookingStatus.REJECTED;
        return new BookingRejected(this.id, this.giftCardReference.value(), amount);
    }

    public BookingId id() {
        return id;
    }

    public Description description() {
        return description;
    }

    public GiftCardReference giftCardReference() {
        return giftCardReference;
    }

    public BookingStatus status() {
        return status;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        return Objects.equals(id, ((Booking) o).id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "Booking[" +
            "id=" + id + ", " +
            "description=" + description + ", " +
            "giftCardReference=" + giftCardReference + ", " +
            "status=" + status +
            ']';
    }
}
