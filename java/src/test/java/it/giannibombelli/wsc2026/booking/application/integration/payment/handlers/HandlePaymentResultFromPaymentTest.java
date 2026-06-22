package it.giannibombelli.wsc2026.booking.application.integration.payment.handlers;

import it.giannibombelli.wsc2026.booking.application.integration.payment.adapter.PaymentResult;
import it.giannibombelli.wsc2026.booking.application.usecases.BookingConfirming;
import it.giannibombelli.wsc2026.booking.application.usecases.BookingRejecting;
import it.giannibombelli.wsc2026.booking.domain.booking.Booking;
import it.giannibombelli.wsc2026.booking.domain.booking.BookingStatus;
import it.giannibombelli.wsc2026.booking.domain.ports.BookingRepository;
import it.giannibombelli.wsc2026.booking.infrastructure.InMemoryBookingEventBus;
import it.giannibombelli.wsc2026.booking.infrastructure.SqliteBookingRepository;
import it.giannibombelli.wsc2026.common.domain.primitive.Money;
import it.giannibombelli.wsc2026.payment.integration.PaymentResultIntegrationEvent;
import it.giannibombelli.wsc2026.testsupport.AggregateFactory;
import it.giannibombelli.wsc2026.testsupport.DatabaseSetup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.util.UUID;

import it.giannibombelli.wsc2026.common.errors.DependencyNotProvidedException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HandlePaymentResultFromPaymentTest {

    private BookingRepository repository;
    private HandlePaymentResultFromPayment handler;

    @BeforeEach
    void setUp() {
        DataSource dataSource = DatabaseSetup.initializeInMemoryDb("booking");
        repository = new SqliteBookingRepository(dataSource);
        InMemoryBookingEventBus eventBus = new InMemoryBookingEventBus(Runnable::run);
        PaymentResult paymentResult = new PaymentResult(repository);
        BookingConfirming confirming = new BookingConfirming(repository, eventBus);
        BookingRejecting rejecting = new BookingRejecting(repository, eventBus);
        handler = new HandlePaymentResultFromPayment(paymentResult, confirming, rejecting);
    }

    @Nested
    class Construction {
        @Test
        void rejectNullParameters() {
            PaymentResult paymentResult = new PaymentResult(repository);
            BookingConfirming confirming = new BookingConfirming(repository, new InMemoryBookingEventBus(Runnable::run));
            BookingRejecting rejecting = new BookingRejecting(repository, new InMemoryBookingEventBus(Runnable::run));

            assertThatThrownBy(() -> new HandlePaymentResultFromPayment(null, confirming, rejecting))
                .isInstanceOf(DependencyNotProvidedException.class);
            assertThatThrownBy(() -> new HandlePaymentResultFromPayment(paymentResult, null, rejecting))
                .isInstanceOf(DependencyNotProvidedException.class);
            assertThatThrownBy(() -> new HandlePaymentResultFromPayment(paymentResult, confirming, null))
                .isInstanceOf(DependencyNotProvidedException.class);
        }
    }

    @Nested
    class PaymentResultsHandling {
        @Test
        void onAccepted_shouldConfirmBooking() {
            Booking booking = saveBooking();
            Money amount = new Money(new BigDecimal("50.00"));

            handler.handle(new PaymentResultIntegrationEvent.PaymentAcceptedIntegrationEvent(
                booking.id().value().toString(), amount
            ));

            Booking updated = repository.findById(booking.id()).orElseThrow();
            assertThat(updated.status()).isEqualTo(BookingStatus.CONFIRMED);
        }

        @Test
        void onRejected_shouldRejectBooking() {
            Booking booking = saveBooking();
            Money amount = new Money(new BigDecimal("50.00"));

            handler.handle(new PaymentResultIntegrationEvent.PaymentRejectedIntegrationEvent(
                booking.id().value().toString(), amount, "declined"
            ));

            Booking updated = repository.findById(booking.id()).orElseThrow();
            assertThat(updated.status()).isEqualTo(BookingStatus.REJECTED);
        }

        @Test
        void onExpired_shouldDoNothing() {
            Booking booking = saveBooking();

            handler.handle(new PaymentResultIntegrationEvent.PaymentExpiredIntegrationEvent(
                booking.id().value().toString(), new Money(new BigDecimal("50.00"))
            ));

            Booking updated = repository.findById(booking.id()).orElseThrow();
            assertThat(updated.status()).isEqualTo(BookingStatus.PLACED);
        }

        @Test
        void onAcceptedUnknownBooking_shouldDoNothing() {
            Money amount = new Money(new BigDecimal("50.00"));

            handler.handle(new PaymentResultIntegrationEvent.PaymentAcceptedIntegrationEvent(
                UUID.randomUUID().toString(), amount
            ));

            assertThat(true).isTrue();
        }
    }

    private Booking saveBooking() {
        Booking booking = AggregateFactory.createBooking();
        repository.save(booking);
        return booking;
    }
}
