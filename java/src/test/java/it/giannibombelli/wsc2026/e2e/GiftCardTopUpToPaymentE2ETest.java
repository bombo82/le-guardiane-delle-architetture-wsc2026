package it.giannibombelli.wsc2026.e2e;

import it.giannibombelli.wsc2026.Application;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GiftCardTopUpToPaymentE2ETest {

    private final JavalinTestHelper javalin = new JavalinTestHelper();
    private final BigDecimal topUpAmount = new BigDecimal("50.00");

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
    void topUpShouldCreatePaymentRequest() {
        GiftCardResponse giftCard = api.issueGiftCard();

        api.requestTopUp(giftCard.id(), topUpAmount);

        PaymentDetailsResponse payment = api.findPaymentRequestedFor(giftCard.id().toString());
        api.assertPaymentMatches(payment, payment.id(), giftCard.id().toString(), topUpAmount);

        PaymentDetailsResponse summary = api.getPaymentSummary(payment.id());
        assertThat(summary.id()).isEqualTo(payment.id());
        assertThat(summary.clientReference()).isEqualTo(giftCard.id().toString());
        assertThat(summary.amount()).isEqualByComparingTo(topUpAmount);
        assertThat(summary.status()).isEqualTo("REQUESTED");
    }

    @Test
    void payingTopUpWithPayPalShouldCreditGiftCardBalance() {
        GiftCardResponse giftCard = api.issueGiftCard();
        UUID giftCardId = giftCard.id();

        api.requestTopUp(giftCardId, topUpAmount);

        PaymentDetailsResponse payment = api.findPaymentRequestedFor(giftCardId.toString());
        api.startFullPaymentTransaction(payment.id(), "PayPal", topUpAmount);

        GiftCardResponse updatedCard = api.waitForGiftCardBalanceToEqual(giftCardId, topUpAmount);
        assertThat(updatedCard.balance()).isEqualByComparingTo(topUpAmount);
    }
}
