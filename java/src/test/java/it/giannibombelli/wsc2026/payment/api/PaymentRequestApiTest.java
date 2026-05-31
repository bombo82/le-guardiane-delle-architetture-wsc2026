package it.giannibombelli.wsc2026.payment.api;

import it.giannibombelli.wsc2026.payment.PaymentModule;
import it.giannibombelli.wsc2026.testsupport.DatabaseSetup;
import it.giannibombelli.wsc2026.testsupport.JavalinTestHelper;
import org.junit.jupiter.api.*;
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
class PaymentRequestApiTest {
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

    private PaymentDetailsResponse createPayment(String clientReference, BigDecimal amount) {
        String requestBody = """
            {"paymentId": "%s", "clientReference": "%s", "amount": "%s", "requestedAt": "%s"}
            """.formatted(UUID.randomUUID(), clientReference, amount, Instant.now());
        JavalinTestHelper.TestResponse response = javalin.post("/internals/payments", requestBody);
        assertThat(response.status()).isEqualTo(201);
        return JSON.readValue(response.body(), PaymentDetailsResponse.class);
    }

    @Nested
    class GetById {
        @Test
        void shouldReturn200() {
            String clientReference = UUID.randomUUID().toString();
            PaymentDetailsResponse created = createPayment(clientReference, new BigDecimal("50.00"));

            JavalinTestHelper.TestResponse getResponse = javalin.get("/payments/" + created.id());

            assertThat(getResponse.status()).isEqualTo(200);
        }

        @Test
        void shouldReturnPaymentWithTransactions() {
            String clientReference = UUID.randomUUID().toString();
            PaymentDetailsResponse created = createPayment(clientReference, new BigDecimal("50.00"));

            JavalinTestHelper.TestResponse getResponse = javalin.get("/payments/" + created.id());

            PaymentDetailsResponse readModel = JSON.readValue(getResponse.body(), PaymentDetailsResponse.class);
            assertThat(readModel.id()).isEqualTo(created.id());
            assertThat(readModel.clientReference()).isEqualTo(clientReference);
            assertThat(readModel.status()).isEqualTo("REQUESTED");
            assertThat(readModel.amount()).isEqualByComparingTo(new BigDecimal("50.00"));
            assertThat(readModel.transactions()).isEmpty();
        }

        @Test
        void shouldReturn404() {
            JavalinTestHelper.TestResponse response = javalin.get("/payments/" + UUID.randomUUID());
            assertThat(response.status()).isEqualTo(404);
        }

        @Test
        void shouldReturn400() {
            JavalinTestHelper.TestResponse response = javalin.get("/payments/invalid-uuid");
            assertThat(response.status()).isEqualTo(400);
            assertThat(response.body()).isEqualTo("Invalid payment id format");
        }
    }

    @Nested
    class InternalCreatePayment {
        @Test
        void shouldReturn201() {
            String clientReference = UUID.randomUUID().toString();

            JavalinTestHelper.TestResponse response = javalin.post(
                "/internals/payments",
                """
                    {"paymentId": "%s", "clientReference": "%s", "amount": "50.00", "requestedAt": "%s"}
                    """.formatted(UUID.randomUUID(), clientReference, Instant.now())
            );

            assertThat(response.status()).isEqualTo(201);
            assertThat(response.header("Location")).matches("/payments/[0-9a-fA-F-]{36}");

            PaymentDetailsResponse body = JSON.readValue(response.body(), PaymentDetailsResponse.class);
            assertThat(body.status()).isEqualTo("REQUESTED");
            assertThat(body.transactions()).isEmpty();
        }

        @Test
        void shouldReturn400WhenBodyMissing() {
            JavalinTestHelper.TestResponse response = javalin.post("/internals/payments", "");
            assertThat(response.status()).isEqualTo(400);
        }
    }
}
