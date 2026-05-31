package it.giannibombelli.wsc2026.giftcard.domain.policies;

import it.giannibombelli.wsc2026.booking.domain.booking.Booking;
import it.giannibombelli.wsc2026.booking.domain.events.BookingResultEvents;
import it.giannibombelli.wsc2026.common.domain.identity.EntityId;
import it.giannibombelli.wsc2026.common.domain.primitive.Money;
import it.giannibombelli.wsc2026.giftcard.application.commands.RefundGiftCard;
import it.giannibombelli.wsc2026.giftcard.domain.giftcard.GiftCardId;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static it.giannibombelli.wsc2026.testsupport.AggregateFactory.createBooking;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RefundGiftCardPolicyTest {

    private final RefundGiftCardPolicy policy = new RefundGiftCardPolicy();

    @Test
    void evaluate_onBookingRejected_returnsRefundGiftCard() {
        GiftCardId giftCardId = EntityId.generate(GiftCardId::new);
        Money amount = new Money(new BigDecimal("50.00"));
        Booking booking = createBooking();
        BookingResultEvents.BookingRejected event = booking.reject(giftCardId, amount);

        RefundGiftCard result = policy.evaluate(event);

        assertThat(result).isNotNull();
        assertThat(result.aggregateId()).isEqualTo(giftCardId);
        assertThat(result.amount()).isEqualTo(amount);
    }

    @Test
    void evaluate_onBookingConfirmed_returnsNull() {
        GiftCardId giftCardId = EntityId.generate(GiftCardId::new);
        Money amount = new Money(new BigDecimal("100.00"));
        Booking booking = createBooking();
        BookingResultEvents.BookingConfirmed event = (BookingResultEvents.BookingConfirmed) booking.confirm(giftCardId, amount);

        RefundGiftCard result = policy.evaluate(event);

        assertThat(result).isNull();
    }

    @Test
    void evaluate_onBookingRefused_returnsNull() {
        GiftCardId giftCardId = EntityId.generate(GiftCardId::new);
        Money amount = new Money(new BigDecimal("75.50"));
        Booking booking = createBooking();
        BookingResultEvents.BookingRefused event = new BookingResultEvents.BookingRefused(booking.id(), giftCardId, amount);

        RefundGiftCard result = policy.evaluate(event);

        assertThat(result).isNull();
    }

    @Test
    void evaluate_onNullEvent_throwsException() {
        assertThatThrownBy(() -> policy.evaluate(null))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
