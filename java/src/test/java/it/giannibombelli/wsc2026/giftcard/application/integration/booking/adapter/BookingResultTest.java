package it.giannibombelli.wsc2026.giftcard.application.integration.booking.adapter;

import it.giannibombelli.wsc2026.booking.domain.booking.Booking;
import it.giannibombelli.wsc2026.booking.integration.giftcard.BookingResultIntegrationEvent;
import it.giannibombelli.wsc2026.common.domain.primitive.Money;
import it.giannibombelli.wsc2026.giftcard.application.commands.CreditGiftCard;
import it.giannibombelli.wsc2026.giftcard.application.commands.RefundGiftCard;
import it.giannibombelli.wsc2026.giftcard.domain.giftcard.GiftCardId;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static it.giannibombelli.wsc2026.testsupport.AggregateFactory.createBooking;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BookingResultTest {

    private final BookingResult bookingResult = new BookingResult();

    @Test
    void adaptBookingCompleted_returnsCreditGiftCard() {
        Money amount = new Money(new BigDecimal("50.00"));
        Booking booking = createBooking();
        BookingResultIntegrationEvent event = new BookingResultIntegrationEvent.BookingCompletedIntegrationEvent(
            booking.giftCardReference().value(), amount
        );

        CreditGiftCard result = bookingResult.adapt(event);

        assertThat(result).isNotNull();
        assertThat(result.aggregateId()).isEqualTo(new GiftCardId(booking.giftCardReference().value()));
        assertThat(result.amount()).isEqualTo(amount);
    }

    @Test
    void adaptBookingRefused_returnsCreditGiftCard() {
        Money amount = new Money(new BigDecimal("75.50"));
        Booking booking = createBooking();
        BookingResultIntegrationEvent event = new BookingResultIntegrationEvent.BookingRefusedIntegrationEvent(
            booking.giftCardReference().value(), amount
        );

        CreditGiftCard result = bookingResult.adapt(event);

        assertThat(result).isNotNull();
        assertThat(result.aggregateId()).isEqualTo(new GiftCardId(booking.giftCardReference().value()));
        assertThat(result.amount()).isEqualTo(amount);
    }

    @Test
    void adaptBookingRejected_returnsNullForCredit() {
        Money amount = new Money(new BigDecimal("100.00"));
        Booking booking = createBooking();
        BookingResultIntegrationEvent event = new BookingResultIntegrationEvent.BookingRejectedIntegrationEvent(
            booking.giftCardReference().value(), amount
        );

        CreditGiftCard result = bookingResult.adapt(event);

        assertThat(result).isNull();
    }

    @Test
    void adaptBookingRejected_returnsRefundGiftCard() {
        Money amount = new Money(new BigDecimal("50.00"));
        Booking booking = createBooking();
        BookingResultIntegrationEvent.BookingRejectedIntegrationEvent event =
            new BookingResultIntegrationEvent.BookingRejectedIntegrationEvent(
                booking.giftCardReference().value(), amount
            );

        RefundGiftCard result = bookingResult.adaptRejected(event);

        assertThat(result).isNotNull();
        assertThat(result.aggregateId()).isEqualTo(new GiftCardId(booking.giftCardReference().value()));
        assertThat(result.amount()).isEqualTo(amount);
    }

    @Test
    void adaptNullCreditEvent_throwsException() {
        assertThatThrownBy(() -> bookingResult.adapt((BookingResultIntegrationEvent) null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void adaptNullRefundEvent_throwsException() {
        assertThatThrownBy(() -> bookingResult.adaptRejected(null))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
