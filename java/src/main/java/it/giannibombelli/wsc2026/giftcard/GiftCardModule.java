package it.giannibombelli.wsc2026.giftcard;

import it.giannibombelli.wsc2026.common.utils.Require;

import io.javalin.config.JavalinConfig;
import it.giannibombelli.wsc2026.common.application.events.EventBus;
import it.giannibombelli.wsc2026.common.application.events.EventSubscriber;
import it.giannibombelli.wsc2026.common.module.ApplicationModule;
import it.giannibombelli.wsc2026.giftcard.api.GiftCardApi;
import it.giannibombelli.wsc2026.giftcard.application.query.GiftCardQueryService;
import it.giannibombelli.wsc2026.giftcard.application.integration.booking.adapter.BookingResult;
import it.giannibombelli.wsc2026.giftcard.application.integration.booking.handlers.CreditFromBooking;
import it.giannibombelli.wsc2026.giftcard.application.integration.booking.handlers.RefundFromBooking;
import it.giannibombelli.wsc2026.giftcard.application.integration.payment.adapter.PaymentResult;
import it.giannibombelli.wsc2026.giftcard.application.integration.payment.handlers.ConfirmTopUpFromPayment;
import it.giannibombelli.wsc2026.giftcard.application.usecases.*;
import it.giannibombelli.wsc2026.giftcard.domain.events.GiftCardEvent;
import it.giannibombelli.wsc2026.giftcard.domain.events.GiftCardTopUpRequested;
import it.giannibombelli.wsc2026.giftcard.application.policies.TopUpPaymentRequestPolicy;
import it.giannibombelli.wsc2026.giftcard.infrastructure.InMemoryGiftCardEventBus;
import it.giannibombelli.wsc2026.giftcard.infrastructure.SqliteGiftCardRepository;

import javax.sql.DataSource;
import java.util.List;
import java.util.function.Consumer;

public final class GiftCardModule extends ApplicationModule {
    private final SqliteGiftCardRepository giftCardRepository;
    private final EventBus<GiftCardEvent> eventBus;
    private final PaymentResult paymentResult;
    private final ConfirmTopUpFromPayment confirmTopUpFromPayment;
    private final BookingResult bookingResult;
    private final CreditFromBooking creditFromBooking;
    private final RefundFromBooking refundFromBooking;
    private final TopUpPaymentRequestPolicy topUpPaymentRequestPolicy;
    private final List<Consumer<GiftCardTopUpRequested>> topUpRequestedHandlers;

    public GiftCardModule(DataSource dataSource) {
        this(dataSource, List.of());
    }

    public GiftCardModule(DataSource dataSource, List<Consumer<GiftCardTopUpRequested>> topUpRequestedHandlers) {
        super();
        Require.requireDependency(dataSource, "dataSource");

        this.giftCardRepository = new SqliteGiftCardRepository(dataSource);
        this.eventBus = new InMemoryGiftCardEventBus(Runnable::run);
        this.topUpRequestedHandlers = topUpRequestedHandlers;
        this.bookingResult = new BookingResult();
        this.paymentResult = new PaymentResult();
        registerCrossBcHandlers();
        confirmTopUpFromPayment = createConfirmTopUpFromPayment();
        creditFromBooking = createCreditFromBooking();
        refundFromBooking = createRefundFromBooking();
        topUpPaymentRequestPolicy = new TopUpPaymentRequestPolicy();
    }

    public ConfirmTopUpFromPayment confirmTopUpFromPayment() {
        return confirmTopUpFromPayment;
    }

    public CreditFromBooking creditFromBooking() {
        return creditFromBooking;
    }

    public RefundFromBooking refundFromBooking() {
        return refundFromBooking;
    }

    public TopUpPaymentRequestPolicy topUpPaymentRequestPolicy() {
        return topUpPaymentRequestPolicy;
    }

    public void configure(JavalinConfig config) {
        GiftCardIssuing giftCardIssuing = new GiftCardIssuing(giftCardRepository);
        TopUpRequesting topUpRequesting = new TopUpRequesting(giftCardRepository, eventBus);
        GiftCardQueryService giftCardQueryService = new GiftCardQueryService(giftCardRepository);

        GiftCardApi api = new GiftCardApi(giftCardIssuing, giftCardQueryService, topUpRequesting);
        api.configure(config);
    }

    private void registerCrossBcHandlers() {
        topUpRequestedHandlers.forEach(handler ->
            eventBus.subscribe(GiftCardTopUpRequested.class, (EventSubscriber<GiftCardTopUpRequested>) handler::accept));
    }

    private ConfirmTopUpFromPayment createConfirmTopUpFromPayment() {
        TopUpConfirming topUpConfirming = new TopUpConfirming(giftCardRepository);
        return new ConfirmTopUpFromPayment(paymentResult, topUpConfirming);
    }

    private CreditFromBooking createCreditFromBooking() {
        GiftCardCrediting useCase = new GiftCardCrediting(giftCardRepository);
        return new CreditFromBooking(bookingResult, useCase);
    }

    private RefundFromBooking createRefundFromBooking() {
        GiftCardRefunding useCase = new GiftCardRefunding(giftCardRepository);
        return new RefundFromBooking(bookingResult, useCase);
    }
}
