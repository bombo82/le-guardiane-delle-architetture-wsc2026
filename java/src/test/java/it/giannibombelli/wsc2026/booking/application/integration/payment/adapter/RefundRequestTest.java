package it.giannibombelli.wsc2026.booking.application.integration.payment.adapter;

import it.giannibombelli.wsc2026.common.domain.identity.EntityId;
import it.giannibombelli.wsc2026.common.domain.primitive.Money;
import it.giannibombelli.wsc2026.booking.domain.booking.BookingId;
import it.giannibombelli.wsc2026.booking.domain.events.BookingResultEvents;
import it.giannibombelli.wsc2026.payment.integration.RefundRequestIntegrationCommand;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RefundRequestTest {

    @Test
    void fromBookingRefused_returnsIntegrationCommand() {
        BookingId bookingId = EntityId.generate(BookingId::new);
        UUID giftCardReference = UUID.randomUUID();
        Money amount = new Money(new BigDecimal("30.00"));
        BookingResultEvents.BookingRefused event = new BookingResultEvents.BookingRefused(bookingId, giftCardReference, amount);

        RefundRequestIntegrationCommand command = RefundRequest.fromBookingRefused(event);

        assertThat(command.clientReference()).isEqualTo(bookingId.value().toString());
        assertThat(command.amount()).isEqualTo(amount);
    }

    @Test
    void fromBookingRefused_withNullEvent_throwsException() {
        assertThatThrownBy(() -> RefundRequest.fromBookingRefused(null))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
