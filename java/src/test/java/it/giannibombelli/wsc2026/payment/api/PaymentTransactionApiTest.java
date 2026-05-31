package it.giannibombelli.wsc2026.payment.api;

import it.giannibombelli.wsc2026.payment.PaymentModule;
import it.giannibombelli.wsc2026.testsupport.DatabaseSetup;
import it.giannibombelli.wsc2026.testsupport.JavalinTestHelper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import tools.jackson.databind.ObjectMapper;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test black-box del contratto HTTP; i payment sono creati tramite l'endpoint interno
 * per mantenere il test al boundary HTTP.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PaymentTransactionApiTest {
    private static final ObjectMapper JSON = new ObjectMapper();
    private final JavalinTestHelper javalin = new JavalinTestHelper();

    @BeforeAll
    void setUp() {
        final DataSource dataSource = DatabaseSetup.initializeFileDb("payment", getClass().getSimpleName());
        PaymentModule module = new PaymentModule(dataSource);
        javalin.start(module::configure);
    }

    @AfterAll
    void tearDown() {
        javalin.stop();
    }

    private String createPayment(String clientReference, BigDecimal amount) {
        String requestBody = """
            {"paymentId": "%s", "clientReference": "%s", "amount": "%s", "requestedAt": "%s"}
            """.formatted(UUID.randomUUID(), clientReference, amount, Instant.now());
        JavalinTestHelper.TestResponse response = javalin.post("/internals/payments", requestBody);
        assertThat(response.status()).isEqualTo(201);
        return response.header("Location");
    }

    private String transactionRequest(String provider, UUID providerReference, BigDecimal amount) {
        if (providerReference != null) {
            return """
                {"provider": "%s", "providerReference": "%s", "amount": %s}
                """.formatted(provider, providerReference, amount);
        }
        return """
            {"provider": "%s", "amount": %s}
            """.formatted(provider, amount);
    }

    @Test
    void shouldAcceptWithPayPal() {
        String clientReference = UUID.randomUUID().toString();
        String location = createPayment(clientReference, new BigDecimal("50.00"));
        UUID providerReference = UUID.randomUUID();

        JavalinTestHelper.TestResponse response = javalin.post(
            location + "/transactions",
            transactionRequest("PayPal", providerReference, new BigDecimal("50.00")));

        assertThat(response.status()).isEqualTo(202);
        TransactionResponse result = JSON.readValue(response.body(), TransactionResponse.class);
        assertThat(result.provider()).isEqualTo("PayPal");
        assertThat(result.providerReference()).isEqualTo(providerReference);
        assertThat(result.amount()).isEqualByComparingTo(new BigDecimal("50.00"));
        assertThat(result.status()).isEqualTo("STARTED");
    }

    @Test
    void shouldAcceptWithKlarna() {
        String clientReference = UUID.randomUUID().toString();
        String location = createPayment(clientReference, new BigDecimal("75.00"));
        UUID providerReference = UUID.randomUUID();

        JavalinTestHelper.TestResponse response = javalin.post(
            location + "/transactions",
            transactionRequest("Klarna", providerReference, new BigDecimal("75.00")));

        assertThat(response.status()).isEqualTo(202);
        TransactionResponse result = JSON.readValue(response.body(), TransactionResponse.class);
        assertThat(result.provider()).isEqualTo("Klarna");
        assertThat(result.providerReference()).isEqualTo(providerReference);
        assertThat(result.amount()).isEqualByComparingTo(new BigDecimal("75.00"));
        assertThat(result.status()).isEqualTo("STARTED");
    }

    @Test
    void shouldRejectWithGiftCardNoReference() {
        String clientReference = UUID.randomUUID().toString();
        String location = createPayment(clientReference, new BigDecimal("30.00"));

        JavalinTestHelper.TestResponse response = javalin.post(
            location + "/transactions",
            transactionRequest("GiftCard", null, new BigDecimal("30.00")));

        assertThat(response.status()).isEqualTo(202);
        TransactionResponse result = JSON.readValue(response.body(), TransactionResponse.class);
        assertThat(result.provider()).isEqualTo("GiftCard");
        assertThat(result.providerReference()).isNull();
        assertThat(result.amount()).isEqualByComparingTo(new BigDecimal("30.00"));
        assertThat(result.status()).isEqualTo("STARTED");
    }

    @Test
    void shouldAcceptWithGiftCardAndReference() {
        String clientReference = UUID.randomUUID().toString();
        String location = createPayment(clientReference, new BigDecimal("30.00"));
        UUID providerReference = UUID.randomUUID();

        JavalinTestHelper.TestResponse response = javalin.post(
            location + "/transactions",
            transactionRequest("GiftCard", providerReference, new BigDecimal("30.00")));

        assertThat(response.status()).isEqualTo(202);
        TransactionResponse result = JSON.readValue(response.body(), TransactionResponse.class);
        assertThat(result.provider()).isEqualTo("GiftCard");
        assertThat(result.providerReference()).isEqualTo(providerReference);
        assertThat(result.amount()).isEqualByComparingTo(new BigDecimal("30.00"));
        assertThat(result.status()).isEqualTo("STARTED");
    }

    @Test
    void shouldReturnPaymentWithTransactionAfterStart() {
        String clientReference = UUID.randomUUID().toString();
        String location = createPayment(clientReference, new BigDecimal("40.00"));

        JavalinTestHelper.TestResponse transactionResponse = javalin.post(
            location + "/transactions",
            transactionRequest("PayPal", UUID.randomUUID(), new BigDecimal("40.00")));

        assertThat(transactionResponse.status()).isEqualTo(202);
        TransactionResponse transaction = JSON.readValue(transactionResponse.body(), TransactionResponse.class);

        JavalinTestHelper.TestResponse paymentResponse = javalin.get(location);
        assertThat(paymentResponse.status()).isEqualTo(200);

        PaymentDetailsResponse payment = JSON.readValue(paymentResponse.body(), PaymentDetailsResponse.class);
        assertThat(payment.transactions()).hasSize(1);
        assertThat(payment.transactions().get(0).id()).isEqualTo(transaction.id());
        assertThat(payment.transactions().get(0).provider()).isEqualTo("PayPal");
    }

    @Test
    void shouldFailIfProviderMissing() {
        String clientReference = UUID.randomUUID().toString();
        String location = createPayment(clientReference, new BigDecimal("50.00"));

        JavalinTestHelper.TestResponse response = javalin.post(
            location + "/transactions",
            "{\"amount\": 50.00}");

        assertThat(response.status()).isEqualTo(400);
        assertThat(response.body()).isEqualTo("provider is required");
    }

    @Test
    void shouldFailIfAmountMissing() {
        String clientReference = UUID.randomUUID().toString();
        String location = createPayment(clientReference, new BigDecimal("50.00"));

        JavalinTestHelper.TestResponse response = javalin.post(
            location + "/transactions",
            "{\"provider\": \"PayPal\"}");

        assertThat(response.status()).isEqualTo(400);
        assertThat(response.body()).isEqualTo("amount is required");
    }

    @Test
    void shouldFailIfPaymentIdInvalid() {
        JavalinTestHelper.TestResponse response = javalin.post(
            "/payments/invalid-uuid/transactions",
            transactionRequest("PayPal", UUID.randomUUID(), new BigDecimal("10.00")));

        assertThat(response.status()).isEqualTo(400);
        assertThat(response.body()).isEqualTo("Invalid payment id format");
    }

    @Test
    void shouldFailIfPaymentNotFound() {
        JavalinTestHelper.TestResponse response = javalin.post(
            "/payments/" + UUID.randomUUID() + "/transactions",
            transactionRequest("PayPal", UUID.randomUUID(), new BigDecimal("10.00")));

        assertThat(response.status()).isEqualTo(404);
    }

    @Test
    void shouldFailIfProviderUnknown() {
        String clientReference = UUID.randomUUID().toString();
        String location = createPayment(clientReference, new BigDecimal("50.00"));

        JavalinTestHelper.TestResponse response = javalin.post(
            location + "/transactions",
            transactionRequest("Unknown", UUID.randomUUID(), new BigDecimal("50.00")));

        assertThat(response.status()).isEqualTo(400);
    }
}
