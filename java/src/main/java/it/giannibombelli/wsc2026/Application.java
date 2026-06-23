package it.giannibombelli.wsc2026;

import it.giannibombelli.wsc2026.common.errors.DependencyNotProvidedException;
import it.giannibombelli.wsc2026.common.utils.Require;

import io.javalin.config.JavalinConfig;
import it.giannibombelli.wsc2026.booking.BookingModule;
import it.giannibombelli.wsc2026.giftcard.GiftCardModule;
import it.giannibombelli.wsc2026.payment.PaymentModule;

import javax.sql.DataSource;
import java.util.Map;

/**
 * Composition root condiviso tra {@link Main} e i test E2E.
 * Accetta esplicitamente i DataSource per mantenere i test isolati.
 */
public final class Application {

    private final BookingModule bookingModule;
    private final GiftCardModule giftCardModule;
    private final PaymentModule paymentModule;

    public Application(DataSource bookingDataSource, DataSource giftCardDataSource, DataSource paymentDataSource) {
        Require.requireDependency(bookingDataSource, "bookingDataSource");
        Require.requireDependency(giftCardDataSource, "giftCardDataSource");
        Require.requireDependency(paymentDataSource, "paymentDataSource");

        this.paymentModule = new PaymentModule(paymentDataSource);
        this.giftCardModule = new GiftCardModule(giftCardDataSource);
        this.bookingModule = new BookingModule(bookingDataSource);

        wireTopUpRequests();
        wireBookingResults();
        wirePaymentResults();
    }

    private void wirePaymentResults() {
        paymentModule.onPaymentResult(bookingModule::handlePaymentResult);
        paymentModule.onPaymentResult(giftCardModule::handlePaymentResult);
    }

    private void wireBookingResults() {
        bookingModule.onBookingCompletedIntegration(giftCardModule::onBookingCompleted);
        bookingModule.onBookingRefusedIntegration(giftCardModule::onBookingRefused);
        bookingModule.onBookingRejectedIntegration(giftCardModule::onBookingRejected);
    }

    private void wireTopUpRequests() {
        giftCardModule.onTopUpRequested(paymentModule::requestPayment);
        bookingModule.onBookingPlaced(paymentModule::requestPayment);
        bookingModule.onBookingRefused(paymentModule::requestRefund);
    }

    public void configure(JavalinConfig config) {
        config.routes.exception(IllegalArgumentException.class, (e, ctx) ->
            ctx.status(400).json(Map.of("error", e.getMessage()))
        );
        config.routes.exception(DependencyNotProvidedException.class, (e, ctx) ->
            ctx.status(500).json(Map.of("error", e.getMessage()))
        );
        config.routes.exception(Exception.class, (e, ctx) ->
            ctx.status(500).json(Map.of("error", "unexpected error"))
        );

        bookingModule.webApis().forEach(api -> api.configure(config));
        giftCardModule.webApis().forEach(api -> api.configure(config));
        paymentModule.webApis().forEach(api -> api.configure(config));
        paymentModule.start();
    }

    public void stop() {
        bookingModule.stop();
        giftCardModule.stop();
        paymentModule.stop();
    }
}
