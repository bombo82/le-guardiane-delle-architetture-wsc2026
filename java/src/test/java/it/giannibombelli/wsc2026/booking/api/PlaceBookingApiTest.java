package it.giannibombelli.wsc2026.booking.api;

import it.giannibombelli.wsc2026.booking.BookingModule;
import it.giannibombelli.wsc2026.testsupport.DatabaseSetup;
import it.giannibombelli.wsc2026.testsupport.JavalinTestHelper;
import org.junit.jupiter.api.*;
import tools.jackson.databind.ObjectMapper;

import javax.sql.DataSource;
import java.util.UUID;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PlaceBookingApiTest {
    private static final ObjectMapper JSON = new ObjectMapper();
    private final JavalinTestHelper javalin = new JavalinTestHelper();

    @BeforeAll
    void setUp() {
        final DataSource dataSource = DatabaseSetup.initializeFileDb("booking", getClass().getSimpleName());
        final BookingModule module = new BookingModule(dataSource);

        javalin.start(config -> module.webApis().forEach(api -> api.configure(config)));
    }

    @AfterAll
    void tearDown() {
        javalin.stop();
    }

    private String requestBody(String amount, String description) {
        return format("{\"amount\": \"%s\", \"description\": \"%s\", \"giftCardId\": \"%s\"}", amount, description, UUID.randomUUID());
    }

    @Nested
    class PostPlace {
        @Test
        void shouldReturn201() {
            JavalinTestHelper.TestResponse postResponse = javalin.post("/bookings", requestBody("99.99", "Nice trip"));

            assertThat(postResponse.status()).isEqualTo(201);
        }

        @Test
        void shouldReturnLocationHeader() {
            JavalinTestHelper.TestResponse postResponse = javalin.post("/bookings", requestBody("50.00", "Happy journey"));

            String location = postResponse.header("Location");
            assertThat(location).matches("/bookings/[0-9a-fA-F-]{36}");
        }

        @Test
        void shouldReturnCreatedBooking() {
            JavalinTestHelper.TestResponse postResponse = javalin.post("/bookings", requestBody("123.45", "Adventure awaits"));

            BookingResponse created = JSON.readValue(postResponse.body(), BookingResponse.class);
            assertThat(created.id()).isNotNull();
            assertThat(created.description()).isEqualTo("Adventure awaits");
            assertThat(created.giftCardId()).isNotNull();
        }

        @Test
        void shouldFailIfAmountMissing() {
            JavalinTestHelper.TestResponse response = javalin.post("/bookings", "{\"description\": \"missing amount\", \"giftCardId\": \"" + UUID.randomUUID() + "\"}");
            assertThat(response.status()).isEqualTo(400);
            assertThat(response.body()).isEqualTo("amount is required");
        }

        @Test
        void shouldFailIfDescriptionMissing() {
            JavalinTestHelper.TestResponse response = javalin.post("/bookings", "{\"amount\": \"100.00\", \"giftCardId\": \"" + UUID.randomUUID() + "\"}");
            assertThat(response.status()).isEqualTo(400);
            assertThat(response.body()).isEqualTo("description is required");
        }

        @Test
        void shouldFailIfGiftCardIdMissing() {
            JavalinTestHelper.TestResponse response = javalin.post("/bookings", "{\"amount\": \"100.00\", \"description\": \"missing gift card\"}");
            assertThat(response.status()).isEqualTo(400);
            assertThat(response.body()).isEqualTo("giftCardId is required");
        }

        @Test
        void shouldFailIfBodyMissing() {
            JavalinTestHelper.TestResponse response = javalin.post("/bookings", "");
            assertThat(response.status()).isEqualTo(400);
            assertThat(response.body()).isEqualTo("request body is required");
        }

        @Test
        void shouldFailIfAmountIsNegative() {
            String requestBody = format("""
                {"amount": "-10.00", "description": "Negative test", "giftCardId": "%s"}
                """, UUID.randomUUID());

            JavalinTestHelper.TestResponse response = javalin.post("/bookings", requestBody);
            assertThat(response.status()).isEqualTo(400);
            assertThat(response.body()).contains("value must not be negative");
        }
    }

    @Nested
    class GetById {
        @Test
        void shouldReturn200() {
            JavalinTestHelper.TestResponse postResponse = javalin.post("/bookings", requestBody("10.00", "Get test"));
            String location = postResponse.header("Location");

            JavalinTestHelper.TestResponse getResponse = javalin.get(location);

            assertThat(getResponse.status()).isEqualTo(200);
        }

        @Test
        void shouldReturnBooking() {
            JavalinTestHelper.TestResponse postResponse = javalin.post("/bookings", requestBody("10.00", "Round trip"));
            BookingResponse created = JSON.readValue(postResponse.body(), BookingResponse.class);
            String location = postResponse.header("Location");

            JavalinTestHelper.TestResponse getResponse = javalin.get(location);

            BookingResponse readModel = JSON.readValue(getResponse.body(), BookingResponse.class);
            assertThat(readModel.id()).isEqualTo(created.id());
            assertThat(readModel.description()).isEqualTo(created.description());
            assertThat(readModel.giftCardId()).isEqualTo(created.giftCardId());
        }

        @Test
        void shouldReturn404() {
            JavalinTestHelper.TestResponse response = javalin.get("/bookings/" + UUID.randomUUID());
            assertThat(response.status()).isEqualTo(404);
        }

        @Test
        void shouldReturn400() {
            JavalinTestHelper.TestResponse response = javalin.get("/bookings/invalid-uuid");
            assertThat(response.status()).isEqualTo(400);
            assertThat(response.body()).isEqualTo("Invalid booking id format");
        }
    }
}
