package it.giannibombelli.wsc2026.booking.domain.booking;

import it.giannibombelli.wsc2026.common.utils.Require;

import it.giannibombelli.wsc2026.booking.domain.events.BookingResultEvents;
import it.giannibombelli.wsc2026.common.domain.model.Aggregate;
import it.giannibombelli.wsc2026.common.domain.primitive.Description;
import it.giannibombelli.wsc2026.common.domain.primitive.Money;
import it.giannibombelli.wsc2026.giftcard.domain.giftcard.GiftCardId;

import java.util.Objects;

import static it.giannibombelli.wsc2026.booking.domain.events.BookingResultEvents.*;


public final class Booking implements Aggregate<BookingId> {
    private final BookingId id;
    private final Description description;
    private final GiftCardId giftCardId;
    private BookingStatus status;

    public Booking(BookingId id, Description description, GiftCardId giftCardId, BookingStatus status) {
        Require.requireArgument(id, "id");
        Require.requireArgument(description, "description");
        Require.requireArgument(giftCardId, "giftCardId");
        Require.requireArgument(status, "status");

        this.id = id;
        this.description = description;
        this.giftCardId = giftCardId;
        this.status = status;
    }

    public static Booking place(BookingId id, Description description, GiftCardId giftCardId) {
        Require.requireArgument(id, "id");
        Require.requireArgument(description, "description");
        Require.requireArgument(giftCardId, "giftCardId");

        return new Booking(id, description, giftCardId, BookingStatus.PLACED);
    }

    public BookingResultEvents confirm(GiftCardId giftCardId, Money amount) {
        Require.requireArgument(giftCardId, "giftCardId");
        Require.requireArgument(amount, "amount");

        // TODO: implement business rule for confirmation vs refusal
        // (e.g. check room availability, overbooking policy, etc.)
        boolean canConfirm = true;

        if (canConfirm) {
            status = BookingStatus.CONFIRMED;
            return new BookingConfirmed(this.id, giftCardId, amount);
        } else {
            status = BookingStatus.REFUSED;
            return new BookingRefused(this.id, giftCardId, amount);
        }
    }

    public BookingRejected reject(GiftCardId giftCardId, Money amount) {
        Require.requireArgument(giftCardId, "giftCardId");
        Require.requireArgument(amount, "amount");

        status = BookingStatus.REJECTED;
        return new BookingRejected(this.id, giftCardId, amount);
    }

    public BookingId id() {
        return id;
    }

    public Description description() {
        return description;
    }

    public GiftCardId giftCardId() {
        return giftCardId;
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
            "giftCardId=" + giftCardId + ", " +
            "status=" + status +
            ']';
    }
}
