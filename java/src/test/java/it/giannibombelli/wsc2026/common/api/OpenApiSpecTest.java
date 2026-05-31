package it.giannibombelli.wsc2026.common.api;

import io.javalin.Javalin;
import io.javalin.json.JavalinJackson3;
import io.javalin.openapi.plugin.OpenApiPlugin;
import io.javalin.openapi.plugin.swagger.SwaggerPlugin;
import it.giannibombelli.wsc2026.Application;
import it.giannibombelli.wsc2026.testsupport.DatabaseSetup;
import it.giannibombelli.wsc2026.testsupport.JavalinTestHelper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifica che la specifica OpenAPI e Swagger UI siano esposte correttamente.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OpenApiSpecTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    private final JavalinTestHelper javalin = new JavalinTestHelper();
    private Application application;

    @BeforeAll
    void setUp() {
        DataSource bookingDataSource = DatabaseSetup.initializeFileDb("booking", getClass().getSimpleName());
        DataSource giftCardDataSource = DatabaseSetup.initializeFileDb("giftcard", getClass().getSimpleName());
        DataSource paymentDataSource = DatabaseSetup.initializeFileDb("payment", getClass().getSimpleName());

        application = new Application(bookingDataSource, giftCardDataSource, paymentDataSource);

        javalin.start(config -> {
            config.jsonMapper(new JavalinJackson3());
            application.configure(config);

            config.registerPlugin(new OpenApiPlugin(openApiConfig ->
                openApiConfig.withDefinitionConfiguration((version, builder) ->
                    builder
                        .info(info -> {
                            info.title("WSC2026 API");
                            info.version("1.0");
                        })
                )
            ));
            config.registerPlugin(new SwaggerPlugin());
        });
    }

    @AfterAll
    void tearDown() {
        javalin.stop();
        application.stop();
    }

    @Test
    void shouldExposeOpenApiSpec() throws Exception {
        JavalinTestHelper.TestResponse response = javalin.get("/openapi");

        assertThat(response.status()).isEqualTo(200);

        JsonNode spec = JSON.readTree(response.body());
        assertThat(spec.get("openapi").asText()).isEqualTo("3.1.0");
        assertThat(spec.get("info").get("title").asText()).isEqualTo("WSC2026 API");
        assertThat(spec.get("paths").has("/bookings")).isTrue();
        assertThat(spec.get("paths").has("/gift-cards")).isTrue();
        assertThat(spec.get("paths").has("/payments/{id}")).isTrue();
        assertThat(spec.get("paths").has("/internals/payments")).isTrue();
        assertThat(spec.get("components").get("schemas").has("PlaceBookingRequest")).isTrue();
        assertThat(spec.get("components").get("schemas").has("BookingResponse")).isTrue();
        assertThat(spec.get("components").get("schemas").has("GiftCardResponse")).isTrue();
        assertThat(spec.get("components").get("schemas").has("PaymentDetailsResponse")).isTrue();
    }

    @Test
    void shouldExposeSwaggerUi() {
        JavalinTestHelper.TestResponse response = javalin.get("/swagger");

        assertThat(response.status()).isEqualTo(200);
        assertThat(response.body()).contains("swagger-ui");
    }
}
