package it.giannibombelli.wsc2026.booking.application.integration.payment.adapter;

import it.giannibombelli.wsc2026.booking.application.commands.BookingConfirmationCommands;
import it.giannibombelli.wsc2026.booking.domain.booking.Booking;
import it.giannibombelli.wsc2026.booking.domain.booking.BookingId;
import it.giannibombelli.wsc2026.booking.domain.ports.BookingRepository;
import it.giannibombelli.wsc2026.booking.infrastructure.SqliteBookingRepository;
import it.giannibombelli.wsc2026.common.domain.primitive.Money;
import it.giannibombelli.wsc2026.payment.integration.PaymentResultIntegrationEvent;
import it.giannibombelli.wsc2026.testsupport.AggregateFactory;
import it.giannibombelli.wsc2026.testsupport.DatabaseSetup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentResultTest {

    private BookingRepository repository;
    private PaymentResult paymentResult;

    @BeforeEach
    void setUp() {
        DataSource dataSource = DatabaseSetup.initializeInMemoryDb("booking");
        repository = new SqliteBookingRepository(dataSource);
        paymentResult = new PaymentResult(repository);
    }

    @Test
    void adaptPaymentAccepted_returnsConfirmBooking() {
        Booking booking = saveBooking();
        Money amount = new Money(new BigDecimal("50.00"));
        PaymentResultIntegrationEvent event = new PaymentResultIntegrationEvent.PaymentAcceptedIntegrationEvent(
            booking.id().value().toString(), amount
        );

        BookingConfirmationCommands cmd = paymentResult.adapt(event);

        assertThat(cmd).isNotNull();
        assertThat(cmd).isInstanceOf(BookingConfirmationCommands.ConfirmBooking.class);
        assertThat(cmd.aggregateId()).isEqualTo(booking.id());
        assertThat(((BookingConfirmationCommands.ConfirmBooking) cmd).giftCardReference()).isEqualTo(booking.giftCardReference());
        assertThat(((BookingConfirmationCommands.ConfirmBooking) cmd).amount()).isEqualTo(amount);
    }

    @Test
    void adaptPaymentRejected_returnsRejectBooking() {
        Booking booking = saveBooking();
        Money amount = new Money(new BigDecimal("50.00"));
        PaymentResultIntegrationEvent event = new PaymentResultIntegrationEvent.PaymentRejectedIntegrationEvent(
            booking.id().value().toString(), amount, "declined"
        );

        BookingConfirmationCommands cmd = paymentResult.adapt(event);

        assertThat(cmd).isNotNull();
        assertThat(cmd).isInstanceOf(BookingConfirmationCommands.RejectBooking.class);
        assertThat(cmd.aggregateId()).isEqualTo(booking.id());
        assertThat(((BookingConfirmationCommands.RejectBooking) cmd).giftCardReference()).isEqualTo(booking.giftCardReference());
        assertThat(((BookingConfirmationCommands.RejectBooking) cmd).amount()).isEqualTo(amount);
    }

    @Test
    void adaptPaymentExpired_returnsNull() {
        Booking booking = saveBooking();
        PaymentResultIntegrationEvent event = new PaymentResultIntegrationEvent.PaymentExpiredIntegrationEvent(
            booking.id().value().toString(), new Money(new BigDecimal("50.00"))
        );

        BookingConfirmationCommands cmd = paymentResult.adapt(event);

        assertThat(cmd).isNull();
    }

    @Test
    void adaptUnknownBooking_returnsNull() {
        PaymentResultIntegrationEvent event = new PaymentResultIntegrationEvent.PaymentAcceptedIntegrationEvent(
            UUID.randomUUID().toString(), new Money(new BigDecimal("50.00"))
        );

        BookingConfirmationCommands cmd = paymentResult.adapt(event);

        assertThat(cmd).isNull();
    }

    @Test
    void adaptNullEvent_throwsException() {
        assertThatThrownBy(() -> paymentResult.adapt(null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    private Booking saveBooking() {
        Booking booking = AggregateFactory.createBooking();
        repository.save(booking);
        return booking;
    }
}
