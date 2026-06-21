package it.giannibombelli.wsc2026.giftcard;

import it.giannibombelli.wsc2026.common.utils.Require;

import io.javalin.config.JavalinConfig;
import it.giannibombelli.wsc2026.common.application.events.EventBus;
import it.giannibombelli.wsc2026.common.application.events.EventSubscriber;
import it.giannibombelli.wsc2026.common.module.ApplicationModule;
import it.giannibombelli.wsc2026.giftcard.api.GiftCardApi;
import it.giannibombelli.wsc2026.giftcard.application.services.BookingResultCrediting;
import it.giannibombelli.wsc2026.giftcard.application.services.BookingResultRefunding;
import it.giannibombelli.wsc2026.giftcard.application.services.TopUpConfirmation;
import it.giannibombelli.wsc2026.giftcard.application.usecases.*;
import it.giannibombelli.wsc2026.giftcard.domain.events.GiftCardEvent;
import it.giannibombelli.wsc2026.giftcard.domain.events.GiftCardTopUpRequested;
import it.giannibombelli.wsc2026.giftcard.application.policies.ConfirmTopUpPolicy;
import it.giannibombelli.wsc2026.giftcard.application.policies.CreditGiftCardPolicy;
import it.giannibombelli.wsc2026.giftcard.application.policies.RefundGiftCardPolicy;
import it.giannibombelli.wsc2026.giftcard.application.policies.TopUpPaymentRequestPolicy;
import it.giannibombelli.wsc2026.giftcard.infrastructure.InMemoryGiftCardEventBus;
import it.giannibombelli.wsc2026.giftcard.infrastructure.SqliteGiftCardRepository;

import javax.sql.DataSource;
import java.util.List;
import java.util.function.Consumer;

public final class GiftCardModule extends ApplicationModule {
    private final SqliteGiftCardRepository giftCardRepository;
    private final EventBus<GiftCardEvent> eventBus;
    private final TopUpConfirmation topUpConfirmation;
    private final BookingResultCrediting bookingResultCrediting;
    private final BookingResultRefunding bookingResultRefunding;
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
        registerCrossBcHandlers();
        topUpConfirmation = createTopUpConfirmation();
        bookingResultCrediting = createBookingResultCrediting();
        bookingResultRefunding = createBookingResultRefunding();
        topUpPaymentRequestPolicy = new TopUpPaymentRequestPolicy();
    }

    public TopUpConfirmation topUpConfirmation() {
        return topUpConfirmation;
    }

    public BookingResultCrediting bookingResultCrediting() {
        return bookingResultCrediting;
    }

    public BookingResultRefunding bookingResultRefunding() {
        return bookingResultRefunding;
    }

    public TopUpPaymentRequestPolicy topUpPaymentRequestPolicy() {
        return topUpPaymentRequestPolicy;
    }

    public void configure(JavalinConfig config) {
        GiftCardIssuing giftCardIssuing = new GiftCardIssuing(giftCardRepository);
        TopUpRequesting topUpRequesting = new TopUpRequesting(giftCardRepository, eventBus);

        GiftCardApi api = new GiftCardApi(giftCardIssuing, giftCardRepository, topUpRequesting);
        api.configure(config);
    }

    private void registerCrossBcHandlers() {
        topUpRequestedHandlers.forEach(handler ->
            eventBus.subscribe(GiftCardTopUpRequested.class, (EventSubscriber<GiftCardTopUpRequested>) handler::accept));
    }

    private TopUpConfirmation createTopUpConfirmation() {
        ConfirmTopUpPolicy confirmTopUpPolicy = new ConfirmTopUpPolicy();
        TopUpConfirming topUpConfirming = new TopUpConfirming(giftCardRepository);
        return new TopUpConfirmation(confirmTopUpPolicy, topUpConfirming);
    }

    private BookingResultCrediting createBookingResultCrediting() {
        CreditGiftCardPolicy policy = new CreditGiftCardPolicy();
        GiftCardCrediting useCase = new GiftCardCrediting(giftCardRepository);
        return new BookingResultCrediting(policy, useCase);
    }

    private BookingResultRefunding createBookingResultRefunding() {
        RefundGiftCardPolicy policy = new RefundGiftCardPolicy();
        GiftCardRefunding useCase = new GiftCardRefunding(giftCardRepository);
        return new BookingResultRefunding(policy, useCase);
    }
}
