package it.giannibombelli.wsc2026;

import it.giannibombelli.wsc2026.common.errors.DependencyNotProvidedException;
import it.giannibombelli.wsc2026.common.utils.Require;

import io.javalin.config.JavalinConfig;
import it.giannibombelli.wsc2026.booking.BookingModule;
import it.giannibombelli.wsc2026.booking.domain.events.BookingPlaced;
import it.giannibombelli.wsc2026.booking.domain.events.BookingResultEvents;
import it.giannibombelli.wsc2026.booking.application.policies.BookingPaymentRequestPolicy;
import it.giannibombelli.wsc2026.booking.application.policies.BookingRefundRequestPolicy;
import it.giannibombelli.wsc2026.common.module.ApplicationModule;
import it.giannibombelli.wsc2026.giftcard.GiftCardModule;
import it.giannibombelli.wsc2026.giftcard.domain.events.GiftCardTopUpRequested;
import it.giannibombelli.wsc2026.payment.PaymentModule;
import it.giannibombelli.wsc2026.payment.application.commands.RefundTransaction;
import it.giannibombelli.wsc2026.payment.application.commands.RequestPayment;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

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

        // PaymentModule first: its use cases are needed by the wiring of the other modules.
        // Handlers are added after construction to break the cycle between payment results and refunds.
        this.paymentModule = new PaymentModule(paymentDataSource, List.of(), List.of(), List.of());

        // AtomicReference resolves the GiftCardModule initialization cycle.
        AtomicReference<GiftCardModule> giftCardModuleRef = new AtomicReference<>();
        Consumer<GiftCardTopUpRequested> topUpToPaymentHandler = event -> {
            RequestPayment cmd = giftCardModuleRef.get().topUpPaymentRequestPolicy().evaluate(event);
            paymentModule.paymentRequesting().invoke(cmd);
        };

        this.giftCardModule = new GiftCardModule(giftCardDataSource, List.of(topUpToPaymentHandler));
        giftCardModuleRef.set(this.giftCardModule);

        BookingRefundRequestPolicy bookingRefundRequestPolicy = new BookingRefundRequestPolicy(paymentModule.paymentRepository());
        BookingPaymentRequestPolicy bookingPaymentRequestPolicy = new BookingPaymentRequestPolicy();

        Consumer<BookingPlaced> bookingPlacedHandler = event -> {
            RequestPayment cmd = bookingPaymentRequestPolicy.evaluate(event);
            paymentModule.paymentRequesting().invoke(cmd);
        };

        Consumer<BookingResultEvents> bookingConfirmedHandler = event -> {
            giftCardModule.bookingResultCrediting().handleBookingResults(event);
            if (event instanceof BookingResultEvents.BookingRefused refused) {
                RefundTransaction cmd = bookingRefundRequestPolicy.evaluate(refused);
                paymentModule.refundRequesting().invoke(cmd);
            }
        };

        Consumer<BookingResultEvents.BookingRejected> bookingRejectedHandler =
            giftCardModule.bookingResultRefunding()::handleBookingResults;

        this.bookingModule = new BookingModule(
            bookingDataSource,
            List.of(bookingPlacedHandler),
            List.of(bookingConfirmedHandler),
            List.of(bookingRejectedHandler)
        );

        paymentModule.addAcceptedHandler(bookingModule.paymentResultOutcome()::handlePaymentResults);
        paymentModule.addRejectedHandler(bookingModule.paymentResultOutcome()::handlePaymentResults);
        paymentModule.addExpiredHandler(bookingModule.paymentResultOutcome()::handlePaymentResults);

        paymentModule.addAcceptedHandler(giftCardModule.topUpConfirmation()::handlePaymentResults);
        paymentModule.addRejectedHandler(giftCardModule.topUpConfirmation()::handlePaymentResults);
        paymentModule.addExpiredHandler(giftCardModule.topUpConfirmation()::handlePaymentResults);
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
