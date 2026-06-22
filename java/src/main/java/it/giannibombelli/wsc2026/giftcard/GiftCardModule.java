package it.giannibombelli.wsc2026.giftcard;

import it.giannibombelli.wsc2026.common.utils.Require;

import io.javalin.config.JavalinConfig;
import it.giannibombelli.wsc2026.booking.integration.giftcard.BookingResultIntegrationEvent;
import it.giannibombelli.wsc2026.common.application.events.EventBus;
import it.giannibombelli.wsc2026.common.application.events.EventSubscriber;
import it.giannibombelli.wsc2026.common.module.ApplicationModule;
import it.giannibombelli.wsc2026.giftcard.api.GiftCardApi;
import it.giannibombelli.wsc2026.giftcard.application.integration.booking.adapter.BookingResult;
import it.giannibombelli.wsc2026.giftcard.application.integration.booking.handlers.CreditFromBooking;
import it.giannibombelli.wsc2026.giftcard.application.integration.booking.handlers.RefundFromBooking;
import it.giannibombelli.wsc2026.giftcard.application.integration.payment.adapter.PaymentRequest;
import it.giannibombelli.wsc2026.giftcard.application.integration.payment.adapter.PaymentResult;
import it.giannibombelli.wsc2026.giftcard.application.integration.payment.handlers.ConfirmTopUpFromPayment;
import it.giannibombelli.wsc2026.giftcard.application.query.GiftCardQueryService;
import it.giannibombelli.wsc2026.giftcard.application.usecases.*;
import it.giannibombelli.wsc2026.giftcard.domain.events.GiftCardEvent;
import it.giannibombelli.wsc2026.giftcard.domain.events.GiftCardTopUpRequested;
import it.giannibombelli.wsc2026.giftcard.domain.ports.GiftCardRepository;
import it.giannibombelli.wsc2026.giftcard.infrastructure.InMemoryGiftCardEventBus;
import it.giannibombelli.wsc2026.giftcard.infrastructure.SqliteGiftCardRepository;
import it.giannibombelli.wsc2026.payment.integration.PaymentRequestIntegrationCommand;
import it.giannibombelli.wsc2026.payment.integration.PaymentResultIntegrationEvent;

import javax.sql.DataSource;
import java.util.function.Consumer;

public final class GiftCardModule extends ApplicationModule {
    private final GiftCardRepository giftCardRepository;
    private final EventBus<GiftCardEvent> eventBus;
    private final BookingResult bookingResult;
    private final PaymentResult paymentResult;
    private final CreditFromBooking creditFromBooking;
    private final RefundFromBooking refundFromBooking;
    private final ConfirmTopUpFromPayment confirmTopUpFromPayment;

    public GiftCardModule(DataSource dataSource) {
        Require.requireDependency(dataSource, "dataSource");

        this.giftCardRepository = new SqliteGiftCardRepository(dataSource);
        this.eventBus = new InMemoryGiftCardEventBus(Runnable::run);
        this.bookingResult = new BookingResult();
        this.paymentResult = new PaymentResult();
        this.creditFromBooking = createCreditFromBooking();
        this.refundFromBooking = createRefundFromBooking();
        this.confirmTopUpFromPayment = createConfirmTopUpFromPayment();
    }

    public void onPaymentResult(PaymentResultIntegrationEvent event) {
        Require.requireArgument(event, "event");
        confirmTopUpFromPayment.handle(event);
    }

    public void onTopUpRequested(Consumer<PaymentRequestIntegrationCommand> handler) {
        var guarded = Require.requireDependency(handler, "handler");
        eventBus.subscribe(GiftCardTopUpRequested.class,
            (EventSubscriber<GiftCardTopUpRequested>) event ->
                guarded.accept(PaymentRequest.fromTopUp(event)));
    }

    public void onBookingCompleted(BookingResultIntegrationEvent.BookingCompletedIntegrationEvent event) {
        Require.requireArgument(event, "event");
        creditFromBooking.handle(event);
    }

    public void onBookingRefused(BookingResultIntegrationEvent.BookingRefusedIntegrationEvent event) {
        Require.requireArgument(event, "event");
        creditFromBooking.handle(event);
    }

    public void onBookingRejected(BookingResultIntegrationEvent.BookingRejectedIntegrationEvent event) {
        Require.requireArgument(event, "event");
        refundFromBooking.handle(event);
    }

    public void configure(JavalinConfig config) {
        GiftCardIssuing giftCardIssuing = new GiftCardIssuing(giftCardRepository);
        TopUpRequesting topUpRequesting = new TopUpRequesting(giftCardRepository, eventBus);
        GiftCardQueryService giftCardQueryService = new GiftCardQueryService(giftCardRepository);

        GiftCardApi api = new GiftCardApi(giftCardIssuing, giftCardQueryService, topUpRequesting);
        api.configure(config);
    }

    private CreditFromBooking createCreditFromBooking() {
        GiftCardCrediting useCase = new GiftCardCrediting(giftCardRepository);
        return new CreditFromBooking(bookingResult, useCase);
    }

    private RefundFromBooking createRefundFromBooking() {
        GiftCardRefunding useCase = new GiftCardRefunding(giftCardRepository);
        return new RefundFromBooking(bookingResult, useCase);
    }

    private ConfirmTopUpFromPayment createConfirmTopUpFromPayment() {
        TopUpConfirming useCase = new TopUpConfirming(giftCardRepository);
        return new ConfirmTopUpFromPayment(paymentResult, useCase);
    }
}
