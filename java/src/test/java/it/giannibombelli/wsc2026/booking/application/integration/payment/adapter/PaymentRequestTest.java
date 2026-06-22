package it.giannibombelli.wsc2026.booking.application.integration.payment.adapter;

import it.giannibombelli.wsc2026.common.domain.identity.EntityId;
import it.giannibombelli.wsc2026.common.domain.primitive.Description;
import it.giannibombelli.wsc2026.common.domain.primitive.Money;
import it.giannibombelli.wsc2026.booking.domain.booking.BookingId;
import it.giannibombelli.wsc2026.booking.domain.events.BookingPlaced;
import it.giannibombelli.wsc2026.payment.integration.PaymentRequestIntegrationCommand;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentRequestTest {

    @Test
    void fromBookingPlaced_returnsIntegrationCommand() {
        BookingId bookingId = EntityId.generate(BookingId::new);
        Money amount = new Money(new BigDecimal("75.00"));
        Description description = new Description("suite");
        UUID giftCardReference = UUID.randomUUID();
        BookingPlaced event = new BookingPlaced(bookingId, amount, description, giftCardReference);

        PaymentRequestIntegrationCommand command = PaymentRequest.fromBookingPlaced(event);

        assertThat(command.clientReference()).isEqualTo(bookingId.value().toString());
        assertThat(command.amount()).isEqualTo(amount);
    }

    @Test
    void fromBookingPlaced_withNullEvent_throwsException() {
        assertThatThrownBy(() -> PaymentRequest.fromBookingPlaced(null))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
