package it.giannibombelli.wsc2026.e2e;

import it.giannibombelli.wsc2026.Application;
import it.giannibombelli.wsc2026.booking.api.BookingResponse;
import it.giannibombelli.wsc2026.e2e.support.E2EApiDriver;
import it.giannibombelli.wsc2026.giftcard.api.GiftCardResponse;
import it.giannibombelli.wsc2026.payment.api.PaymentDetailsResponse;
import it.giannibombelli.wsc2026.testsupport.DatabaseSetup;
import it.giannibombelli.wsc2026.testsupport.JavalinTestHelper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import javax.sql.DataSource;
import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BookingToGiftCardCreditE2ETest {

    private final JavalinTestHelper javalin = new JavalinTestHelper();
    private final BigDecimal bookingAmount = new BigDecimal("75.00");

    private Application application;
    private E2EApiDriver api;

    @BeforeAll
    void setUp() {
        DataSource bookingDataSource = DatabaseSetup.initializeFileDb("booking", getClass().getSimpleName());
        DataSource giftCardDataSource = DatabaseSetup.initializeFileDb("giftcard", getClass().getSimpleName());
        DataSource paymentDataSource = DatabaseSetup.initializeFileDb("payment", getClass().getSimpleName());

        application = new Application(bookingDataSource, giftCardDataSource, paymentDataSource);
        api = new E2EApiDriver(javalin);

        javalin.start(application::configure);
    }

    @AfterAll
    void tearDown() {
        javalin.stop();
        application.stop();
    }

    @Test
    void placingBookingShouldCreatePaymentRequest() {
        GiftCardResponse giftCard = api.issueGiftCard();
        BookingResponse booking = api.placeBooking(giftCard.id(), bookingAmount);

        PaymentDetailsResponse payment = api.findPaymentRequestedFor(booking.id().toString());
        api.assertPaymentMatches(payment, payment.id(), booking.id().toString(), bookingAmount);

        PaymentDetailsResponse summary = api.getPaymentSummary(payment.id());
        assertThat(summary.id()).isEqualTo(payment.id());
        assertThat(summary.clientReference()).isEqualTo(booking.id().toString());
        assertThat(summary.amount()).isEqualByComparingTo(bookingAmount);
        assertThat(summary.status()).isEqualTo("REQUESTED");
    }

    @Test
    void payingBookingWithKlarnaShouldCreditGiftCardBalance() {
        GiftCardResponse giftCard = api.issueGiftCard();
        BookingResponse booking = api.placeBooking(giftCard.id(), bookingAmount);

        PaymentDetailsResponse payment = api.findPaymentRequestedFor(booking.id().toString());
        api.startFullPaymentTransaction(payment.id(), "Klarna", bookingAmount);

        GiftCardResponse updatedCard = api.waitForGiftCardBalanceToEqual(giftCard.id(), bookingAmount);
        assertThat(updatedCard.balance()).isEqualByComparingTo(bookingAmount);
    }
}
