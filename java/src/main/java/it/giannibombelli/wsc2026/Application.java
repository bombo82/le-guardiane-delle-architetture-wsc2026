package it.giannibombelli.wsc2026;

import it.giannibombelli.wsc2026.common.errors.DependencyNotProvidedException;
import it.giannibombelli.wsc2026.common.utils.Require;

import io.javalin.config.JavalinConfig;
import it.giannibombelli.wsc2026.booking.BookingModule;
import it.giannibombelli.wsc2026.booking.domain.events.BookingResultEvents;
import it.giannibombelli.wsc2026.giftcard.GiftCardModule;
import it.giannibombelli.wsc2026.giftcard.application.integration.payment.adapter.PaymentRequest;
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
        paymentModule.onPaymentResult(bookingModule.handlePaymentResultFromPayment()::handle);
        paymentModule.onPaymentResult(giftCardModule.confirmTopUpFromPayment()::handle);
    }

    private void wireBookingResults() {
        bookingModule.onBookingResultIntegration(giftCardModule.creditFromBooking()::handle);
        bookingModule.onBookingRejectedIntegration(giftCardModule.refundFromBooking()::handle);
    }

    private void wireTopUpRequests() {
        giftCardModule.onTopUpRequested(event ->
            paymentModule.requestPayment(PaymentRequest.fromTopUp(event))
        );

        bookingModule.onBookingPlaced(event ->
            paymentModule.requestPayment(
                it.giannibombelli.wsc2026.booking.application.integration.payment.adapter.PaymentRequest.fromBookingPlaced(event)
            )
        );

        bookingModule.onBookingResult(event -> {
            if (event instanceof BookingResultEvents.BookingRefused refused) {
                paymentModule.requestRefund(
                    it.giannibombelli.wsc2026.booking.application.integration.payment.adapter.RefundRequest.fromBookingRefused(refused)
                );
            }
        });
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

        bookingModule.configure(config);
        giftCardModule.configure(config);
        paymentModule.configure(config);
    }

    public void stop() {
        bookingModule.stop();
        giftCardModule.stop();
        paymentModule.stop();
    }
}
