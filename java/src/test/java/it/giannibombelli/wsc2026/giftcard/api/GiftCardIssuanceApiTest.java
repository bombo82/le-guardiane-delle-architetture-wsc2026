package it.giannibombelli.wsc2026.giftcard.api;

import it.giannibombelli.wsc2026.giftcard.GiftCardModule;
import it.giannibombelli.wsc2026.testsupport.DatabaseSetup;
import it.giannibombelli.wsc2026.testsupport.JavalinTestHelper;
import org.junit.jupiter.api.*;
import tools.jackson.databind.ObjectMapper;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GiftCardIssuanceApiTest {
    private static final ObjectMapper JSON = new ObjectMapper();
    private final JavalinTestHelper javalin = new JavalinTestHelper();

    @BeforeAll
    void setUp() {
        final DataSource dataSource = DatabaseSetup.initializeFileDb("giftcard", getClass().getSimpleName());
        GiftCardModule module = new GiftCardModule(dataSource, List.of());

        javalin.start(module::configure);
    }

    @AfterAll
    void tearDown() {
        javalin.stop();
    }

    @Nested
    class PostIssuance {
        @Test
        void shouldReturn201() {
            JavalinTestHelper.TestResponse postResponse = javalin.post("/gift-cards");

            assertThat(postResponse.status()).isEqualTo(201);
        }

        @Test
        void shouldReturnLocationHeader() {
            JavalinTestHelper.TestResponse postResponse = javalin.post("/gift-cards");

            String location = postResponse.header("Location");
            assertThat(location).matches("/gift-cards/[0-9a-fA-F-]{36}");
        }

        @Test
        void shouldReturnCreatedCard() {
            JavalinTestHelper.TestResponse postResponse = javalin.post("/gift-cards");

            GiftCardResponse created = JSON.readValue(postResponse.body(), GiftCardResponse.class);
            assertThat(created.id()).isNotNull();
            assertThat(created.balance()).isEqualByComparingTo(BigDecimal.ZERO);
        }
    }

    @Nested
    class GetById {
        @Test
        void shouldReturn200() {
            JavalinTestHelper.TestResponse postResponse = javalin.post("/gift-cards");
            String location = postResponse.header("Location");

            JavalinTestHelper.TestResponse getResponse = javalin.get(location);

            assertThat(getResponse.status()).isEqualTo(200);
        }

        @Test
        void shouldReturnCard() {
            JavalinTestHelper.TestResponse postResponse = javalin.post("/gift-cards");
            GiftCardResponse created = JSON.readValue(postResponse.body(), GiftCardResponse.class);
            String location = postResponse.header("Location");

            JavalinTestHelper.TestResponse getResponse = javalin.get(location);

            GiftCardResponse readModel = JSON.readValue(getResponse.body(), GiftCardResponse.class);
            assertThat(readModel.id()).isEqualTo(created.id());
            assertThat(readModel.balance()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        void shouldReturn404() {
            JavalinTestHelper.TestResponse response = javalin.get("/gift-cards/" + UUID.randomUUID());
            assertThat(response.status()).isEqualTo(404);
        }

        @Test
        void shouldReturn400() {
            JavalinTestHelper.TestResponse response = javalin.get("/gift-cards/invalid-uuid");
            assertThat(response.status()).isEqualTo(400);
            assertThat(response.body()).isEqualTo("Invalid gift card id format");
        }
    }
}
