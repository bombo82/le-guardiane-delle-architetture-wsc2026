package it.giannibombelli.wsc2026.booking.application.services;

import it.giannibombelli.wsc2026.booking.application.usecases.BookingConfirming;
import it.giannibombelli.wsc2026.booking.application.usecases.BookingRejecting;
import it.giannibombelli.wsc2026.booking.domain.booking.Booking;
import it.giannibombelli.wsc2026.booking.domain.policies.PaymentPolicy;
import it.giannibombelli.wsc2026.booking.domain.ports.BookingRepository;
import it.giannibombelli.wsc2026.booking.infrastructure.SqliteBookingRepository;
import it.giannibombelli.wsc2026.common.domain.primitive.Description;
import it.giannibombelli.wsc2026.common.domain.primitive.Money;
import it.giannibombelli.wsc2026.payment.domain.events.PaymentResultEvents;
import it.giannibombelli.wsc2026.payment.domain.payment.Payment;
import it.giannibombelli.wsc2026.testsupport.AggregateFactory;
import it.giannibombelli.wsc2026.testsupport.DatabaseSetup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.util.Optional;

import static it.giannibombelli.wsc2026.testsupport.AggregateFactory.createPayment;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentResultOutcomeTest {

    private BookingRepository repository;
    private PaymentResultOutcome service;

    @BeforeEach
    void setUp() {
        final DataSource dataSource = DatabaseSetup.initializeInMemoryDb("booking");
        repository = new SqliteBookingRepository(dataSource);

        PaymentPolicy policy = new PaymentPolicy(repository);
        BookingConfirming confirmation = new BookingConfirming(repository, event -> {
        });
        BookingRejecting rejection = new BookingRejecting(repository, event -> {
        });

        service = new PaymentResultOutcome(policy, confirmation, rejection);
    }

    @Nested
    class Construction {
        @Test
        void rejectNullParameters() {
            PaymentPolicy policy = new PaymentPolicy(repository);
            BookingConfirming confirmation = new BookingConfirming(repository, event -> {
            });
            BookingRejecting rejection = new BookingRejecting(repository, event -> {
            });

            assertThatThrownBy(() -> new PaymentResultOutcome(null, confirmation, rejection))
                .isInstanceOf(NullPointerException.class);

            assertThatThrownBy(() -> new PaymentResultOutcome(policy, null, rejection))
                .isInstanceOf(NullPointerException.class);

            assertThatThrownBy(() -> new PaymentResultOutcome(policy, confirmation, null))
                .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    class PaymentResultsHandling {
        @Test
        void onAccepted_shouldConfirmBooking() {
            Booking booking = AggregateFactory.createBooking();
            repository.save(booking);
            Payment payment = createPayment(booking.id().value().toString(), new Money(new BigDecimal("50.00")));
            Money amount = new Money(new BigDecimal("50.00"));

            service.handlePaymentResults(
                new PaymentResultEvents.PaymentAccepted(payment.id(), payment.clientReference(), amount)
            );

            Optional<Booking> updated = repository.findById(booking.id());
            assertThat(updated).isPresent();
        }

        @Test
        void onRejected_shouldRejectBooking() {
            Booking booking = AggregateFactory.createBooking();
            repository.save(booking);
            Payment payment = createPayment(booking.id().value().toString(), new Money(new BigDecimal("50.00")));

            service.handlePaymentResults(
                new PaymentResultEvents.PaymentRejected(payment.id(), payment.clientReference(), payment.amount(), new Description("declined"))
            );

            Optional<Booking> updated = repository.findById(booking.id());
            assertThat(updated).isPresent();
        }

        @Test
        void onExpired_shouldRejectBooking() {
            Booking booking = AggregateFactory.createBooking();
            repository.save(booking);
            Payment payment = createPayment(booking.id().value().toString(), new Money(new BigDecimal("50.00")));

            service.handlePaymentResults(
                new PaymentResultEvents.PaymentExpired(payment.id(), payment.clientReference(), payment.amount())
            );

            Optional<Booking> updated = repository.findById(booking.id());
            assertThat(updated).isPresent();
        }
    }
}
