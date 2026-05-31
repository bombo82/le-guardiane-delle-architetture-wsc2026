package it.giannibombelli.wsc2026.giftcard.api;

import it.giannibombelli.wsc2026.giftcard.GiftCardModule;
import it.giannibombelli.wsc2026.testsupport.DatabaseSetup;
import it.giannibombelli.wsc2026.testsupport.JavalinTestHelper;
import org.junit.jupiter.api.*;
import tools.jackson.databind.ObjectMapper;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test black-box del contratto HTTP; le gift card sono create tramite l'endpoint pubblico
 * per mantenere il test al boundary HTTP.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GiftCardTopUpApiTest {
    private static final ObjectMapper JSON = new ObjectMapper();
    private final JavalinTestHelper javalin = new JavalinTestHelper();

    @BeforeAll
    void setUp() {
        final DataSource dataSource = DatabaseSetup.initializeFileDb("giftcard", getClass().getSimpleName());
        GiftCardModule module = new GiftCardModule(dataSource, java.util.List.of());

        javalin.start(module::configure);
    }

    @AfterAll
    void tearDown() {
        javalin.stop();
    }

    private String createGiftCard() {
        JavalinTestHelper.TestResponse response = javalin.post("/gift-cards");
        assertThat(response.status()).isEqualTo(201);
        return response.header("Location");
    }

    @Nested
    class RequestTopUp {
        @Test
        void shouldReturn200() {
            String location = createGiftCard();
            String requestBody = """
                {"amount": "50.00"}
                """;

            JavalinTestHelper.TestResponse response = javalin.post(location + "/top-up", requestBody);

            assertThat(response.status()).isEqualTo(200);
        }

        @Test
        void shouldReturnUpdatedCard() {
            String location = createGiftCard();
            String requestBody = """
                {"amount": "50.00"}
                """;

            JavalinTestHelper.TestResponse response = javalin.post(location + "/top-up", requestBody);

            GiftCardResponse readModel = JSON.readValue(response.body(), GiftCardResponse.class);
            assertThat(readModel.id()).isEqualTo(UUID.fromString(location.substring(location.lastIndexOf('/') + 1)));
            // Critical didactic signal: balance has NOT increased — the top-up is only requested (pending external payment)
            assertThat(readModel.balance()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        void shouldReturn404() {
            UUID nonExisting = UUID.randomUUID();
            String requestBody = """
                {"amount": "10.00"}
                """;

            JavalinTestHelper.TestResponse response = javalin.post("/gift-cards/" + nonExisting + "/top-up", requestBody);

            assertThat(response.status()).isEqualTo(404);
        }

        @Test
        void shouldFailIfAmountMissing() {
            String location = createGiftCard();

            JavalinTestHelper.TestResponse response = javalin.post(location + "/top-up", "{}");

            assertThat(response.status()).isEqualTo(400);
        }

        @Test
        void shouldFailIfAmountIsInvalid() {
            String location = createGiftCard();

            JavalinTestHelper.TestResponse response = javalin.post(location + "/top-up", "{\"amount\": \"-10.00\"}");

            assertThat(response.status()).isEqualTo(400);
        }

        @Test
        void shouldReturn400() {
            JavalinTestHelper.TestResponse response = javalin.post("/gift-cards/invalid-uuid/top-up", "{\"amount\": \"10\"}");
            assertThat(response.status()).isEqualTo(400);
            assertThat(response.body()).isEqualTo("Invalid gift card id format");
        }

        @Test
        void shouldFailIfBodyMissing() {
            String location = createGiftCard();

            JavalinTestHelper.TestResponse response = javalin.post(location + "/top-up", "");
            assertThat(response.status()).isEqualTo(400);
            assertThat(response.body()).isEqualTo("request body is required");
        }

        @Test
        void shouldFailIfBodyIsNull() {
            String location = createGiftCard();

            JavalinTestHelper.TestResponse response = javalin.post(location + "/top-up", "null");
            assertThat(response.status()).isEqualTo(400);
            assertThat(response.body()).isEqualTo("request body is required");
        }
    }
}
