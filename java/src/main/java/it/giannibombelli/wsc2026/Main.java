package it.giannibombelli.wsc2026;

import io.javalin.Javalin;
import io.javalin.json.JavalinJackson3;
import io.javalin.openapi.plugin.OpenApiPlugin;
import io.javalin.openapi.plugin.swagger.SwaggerPlugin;
import it.giannibombelli.wsc2026.booking.BookingModule;
import it.giannibombelli.wsc2026.common.module.ApplicationModule;
import it.giannibombelli.wsc2026.giftcard.GiftCardModule;
import it.giannibombelli.wsc2026.payment.PaymentModule;

import javax.sql.DataSource;

public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        int port = (args.length > 0) ? Integer.parseInt(args[0]) : 7070;

        DataSource bookingDataSource = BookingModule.initializeDb("booking");
        DataSource giftCardDataSource = GiftCardModule.initializeDb("giftcard");
        DataSource paymentDataSource = PaymentModule.initializeDb("payment");

        Application application = new Application(bookingDataSource, giftCardDataSource, paymentDataSource);

        Javalin app = Javalin.create(config -> {
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

        Runtime.getRuntime().addShutdownHook(new Thread(application::stop));

        app.start(port);

        System.out.println("Application started on http://localhost:" + port);
        System.out.println(" - BookingModule registered");
        System.out.println(" - GiftCardModule registered");
        System.out.println(" - PaymentModule registered");
    }
}
