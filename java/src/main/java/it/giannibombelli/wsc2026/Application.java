package it.giannibombelli.wsc2026;

import it.giannibombelli.wsc2026.common.errors.DependencyNotProvidedException;
import it.giannibombelli.wsc2026.common.utils.Require;

import io.javalin.config.JavalinConfig;
import it.giannibombelli.wsc2026.booking.BookingModule;
import it.giannibombelli.wsc2026.booking.domain.events.BookingResultEvents;
import it.giannibombelli.wsc2026.booking.application.policies.BookingPaymentRequestPolicy;
import it.giannibombelli.wsc2026.booking.application.policies.BookingRefundRequestPolicy;
import it.giannibombelli.wsc2026.common.module.ApplicationModule;
import it.giannibombelli.wsc2026.giftcard.GiftCardModule;
import it.giannibombelli.wsc2026.payment.PaymentModule;
import it.giannibombelli.wsc2026.payment.application.commands.RefundTransaction;
import it.giannibombelli.wsc2026.payment.application.commands.RequestPayment;

import javax.sql.DataSource;
import java.util.Map;

/**
 * Composition root condiviso tra {@link Main} e i test E2E.
 * Accetta esplicitamente i DataSource per mantenere i test isolati.
 */
public final class Application extends ApplicationModule {

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
        giftCardModule.onTopUpRequested(event -> {
            RequestPayment cmd = giftCardModule.topUpPaymentRequestPolicy().evaluate(event);
            paymentModule.paymentRequesting().invoke(cmd);
        });

        BookingPaymentRequestPolicy bookingPaymentRequestPolicy = new BookingPaymentRequestPolicy();
        bookingModule.onBookingPlaced(event -> {
            RequestPayment cmd = bookingPaymentRequestPolicy.evaluate(event);
            paymentModule.paymentRequesting().invoke(cmd);
        });

        BookingRefundRequestPolicy bookingRefundRequestPolicy = new BookingRefundRequestPolicy(paymentModule.paymentRepository());
        bookingModule.onBookingResult(event -> {
            if (event instanceof BookingResultEvents.BookingRefused refused) {
                RefundTransaction cmd = bookingRefundRequestPolicy.evaluate(refused);
                paymentModule.refundRequesting().invoke(cmd);
            }
        });
    }

    @Override
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

    @Override
    public void stop() {
        bookingModule.stop();
        giftCardModule.stop();
        paymentModule.stop();
    }
}
