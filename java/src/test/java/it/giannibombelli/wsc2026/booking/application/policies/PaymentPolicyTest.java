package it.giannibombelli.wsc2026.booking.application.policies;

import it.giannibombelli.wsc2026.booking.application.commands.BookingConfirmationCommands;
import it.giannibombelli.wsc2026.booking.domain.booking.Booking;
import it.giannibombelli.wsc2026.booking.domain.booking.BookingId;
import it.giannibombelli.wsc2026.booking.domain.ports.BookingRepository;
import it.giannibombelli.wsc2026.booking.domain.primitive.GiftCardReference;
import it.giannibombelli.wsc2026.booking.infrastructure.SqliteBookingRepository;
import it.giannibombelli.wsc2026.common.domain.identity.EntityId;
import it.giannibombelli.wsc2026.common.domain.primitive.Description;
import it.giannibombelli.wsc2026.common.domain.primitive.Money;
import it.giannibombelli.wsc2026.payment.domain.events.PaymentResultEvents;
import it.giannibombelli.wsc2026.payment.domain.payment.Payment;
import it.giannibombelli.wsc2026.testsupport.DatabaseSetup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.util.UUID;

import static it.giannibombelli.wsc2026.booking.application.commands.BookingConfirmationCommands.ConfirmBooking;
import static it.giannibombelli.wsc2026.booking.application.commands.BookingConfirmationCommands.RejectBooking;
import static it.giannibombelli.wsc2026.testsupport.AggregateFactory.createPayment;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentPolicyTest {

    private BookingRepository bookingRepository;
    private PaymentPolicy policy;

    @BeforeEach
    void setUp() {
        DataSource dataSource = DatabaseSetup.initializeInMemoryDb("booking");
        bookingRepository = new SqliteBookingRepository(dataSource);
        policy = new PaymentPolicy(bookingRepository);
    }

    @Test
    void evaluate_onPaymentAccepted_returnsConfirmBookingWithGiftCardReferenceFromBooking() {
        BookingId bookingId = EntityId.generate(BookingId::new);
        GiftCardReference giftCardReference = new GiftCardReference(UUID.randomUUID());
        saveBooking(bookingId, giftCardReference);

        Payment payment = createPayment(bookingId.value().toString(), new Money(new BigDecimal("50.00")));
        Money amount = new Money(new BigDecimal("50.00"));
        PaymentResultEvents.PaymentAccepted event = new PaymentResultEvents.PaymentAccepted(
            payment.id(), payment.clientReference(), amount
        );

        final BookingConfirmationCommands result = policy.evaluate(event);

        assertThat(result).isInstanceOf(ConfirmBooking.class);
        ConfirmBooking cmd = (ConfirmBooking) result;
        assertThat(cmd).isNotNull();
        assertThat(cmd.aggregateId()).isEqualTo(bookingId);
        assertThat(cmd.giftCardReference()).isEqualTo(giftCardReference);
        assertThat(cmd.amount()).isEqualTo(amount);
    }

    @Test
    void evaluate_onPaymentAccepted_withNonGiftCardProvider_returnsConfirmBookingWithGiftCardReferenceFromBooking() {
        BookingId bookingId = EntityId.generate(BookingId::new);
        GiftCardReference giftCardReference = new GiftCardReference(UUID.randomUUID());
        saveBooking(bookingId, giftCardReference);

        Payment payment = createPayment(bookingId.value().toString(), new Money(new BigDecimal("50.00")));
        Money amount = new Money(new BigDecimal("50.00"));
        PaymentResultEvents.PaymentAccepted event = new PaymentResultEvents.PaymentAccepted(
            payment.id(), payment.clientReference(), amount
        );

        final BookingConfirmationCommands result = policy.evaluate(event);

        assertThat(result).isInstanceOf(ConfirmBooking.class);
        ConfirmBooking cmd = (ConfirmBooking) result;
        assertThat(cmd.aggregateId()).isEqualTo(bookingId);
        assertThat(cmd.giftCardReference()).isEqualTo(giftCardReference);
        assertThat(cmd.amount()).isEqualTo(amount);
    }

    @Test
    void evaluate_onPaymentRejected_returnsRejectBookingWithGiftCardReferenceFromBooking() {
        BookingId bookingId = EntityId.generate(BookingId::new);
        GiftCardReference giftCardReference = new GiftCardReference(UUID.randomUUID());
        saveBooking(bookingId, giftCardReference);

        Payment payment = createPayment(bookingId.value().toString(), new Money(new BigDecimal("50.00")));
        PaymentResultEvents.PaymentRejected event = new PaymentResultEvents.PaymentRejected(
            payment.id(), payment.clientReference(), payment.amount(), new Description("declined")
        );

        final BookingConfirmationCommands result = policy.evaluate(event);

        assertThat(result).isInstanceOf(RejectBooking.class);
        RejectBooking cmd = (RejectBooking) result;
        assertThat(cmd.aggregateId()).isEqualTo(bookingId);
        assertThat(cmd.giftCardReference()).isEqualTo(giftCardReference);
        assertThat(cmd.amount()).isEqualTo(payment.amount());
    }

    @Test
    void evaluate_onPaymentAccepted_withClientReferenceNotMatchingAnyBooking_returnsNull() {
        Payment payment = createPayment(UUID.randomUUID().toString(), new Money(new BigDecimal("50.00")));
        Money amount = new Money(new BigDecimal("50.00"));
        PaymentResultEvents.PaymentAccepted event = new PaymentResultEvents.PaymentAccepted(
            payment.id(), payment.clientReference(), amount
        );

        final BookingConfirmationCommands result = policy.evaluate(event);

        assertThat(result).isNull();
    }

    @Test
    void evaluate_onPaymentRejected_withClientReferenceNotMatchingAnyBooking_returnsNull() {
        Payment payment = createPayment(UUID.randomUUID().toString(), new Money(new BigDecimal("50.00")));
        PaymentResultEvents.PaymentRejected event = new PaymentResultEvents.PaymentRejected(
            payment.id(), payment.clientReference(), payment.amount(), new Description("declined")
        );

        final BookingConfirmationCommands result = policy.evaluate(event);

        assertThat(result).isNull();
    }

    @Test
    void evaluate_onPaymentExpired_returnsNull() {
        Payment payment = createPayment();
        PaymentResultEvents.PaymentExpired event = new PaymentResultEvents.PaymentExpired(
            payment.id(), payment.clientReference(), payment.amount()
        );

        final BookingConfirmationCommands result = policy.evaluate(event);

        assertThat(result).isNull();
    }

    @Test
    void evaluate_onNullEvent_throwsException() {
        assertThatThrownBy(() -> policy.evaluate(null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    private void saveBooking(BookingId bookingId, GiftCardReference giftCardReference) {
        Booking booking = Booking.place(bookingId, new Description("Test booking"), giftCardReference);
        bookingRepository.save(booking);
    }
}
