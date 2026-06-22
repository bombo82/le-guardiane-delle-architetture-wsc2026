package it.giannibombelli.wsc2026.booking;

import it.giannibombelli.wsc2026.common.utils.Require;

import io.javalin.config.JavalinConfig;
import it.giannibombelli.wsc2026.booking.api.BookingApi;
import it.giannibombelli.wsc2026.booking.application.integration.payment.handlers.HandlePaymentResultFromPayment;
import it.giannibombelli.wsc2026.booking.application.query.BookingQueryService;
import it.giannibombelli.wsc2026.booking.application.usecases.BookingConfirming;
import it.giannibombelli.wsc2026.booking.application.usecases.BookingPlacing;
import it.giannibombelli.wsc2026.booking.application.usecases.BookingRejecting;
import it.giannibombelli.wsc2026.booking.domain.events.BookingEvent;
import it.giannibombelli.wsc2026.booking.domain.events.BookingPlaced;
import it.giannibombelli.wsc2026.booking.domain.events.BookingResultEvents;
import it.giannibombelli.wsc2026.booking.domain.ports.BookingRepository;
import it.giannibombelli.wsc2026.booking.integration.giftcard.BookingResultIntegrationEvent;
import it.giannibombelli.wsc2026.booking.integration.giftcard.BookingResultIntegrationEvent.BookingCompletedIntegrationEvent;
import it.giannibombelli.wsc2026.booking.integration.giftcard.BookingResultIntegrationEvent.BookingRejectedIntegrationEvent;
import it.giannibombelli.wsc2026.booking.integration.giftcard.BookingResultIntegrationEvent.BookingRefusedIntegrationEvent;
import it.giannibombelli.wsc2026.booking.application.integration.payment.adapter.PaymentResult;
import it.giannibombelli.wsc2026.booking.infrastructure.InMemoryBookingEventBus;
import it.giannibombelli.wsc2026.booking.infrastructure.SqliteBookingRepository;
import it.giannibombelli.wsc2026.common.application.events.EventBus;
import it.giannibombelli.wsc2026.common.application.events.EventSubscriber;
import it.giannibombelli.wsc2026.common.module.ApplicationModule;

import javax.sql.DataSource;
import java.util.function.Consumer;

public final class BookingModule extends ApplicationModule {

    private final BookingRepository bookingRepository;
    private final EventBus<BookingEvent> eventBus;
    private final HandlePaymentResultFromPayment handlePaymentResultFromPayment;

    public BookingModule(DataSource dataSource) {
        Require.requireDependency(dataSource, "dataSource");

        this.bookingRepository = new SqliteBookingRepository(dataSource);
        this.eventBus = new InMemoryBookingEventBus(Runnable::run);
        this.handlePaymentResultFromPayment = createHandlePaymentResultFromPayment();
    }

    public HandlePaymentResultFromPayment handlePaymentResultFromPayment() {
        return handlePaymentResultFromPayment;
    }

    public void onBookingPlaced(Consumer<BookingPlaced> handler) {
        eventBus.subscribe(BookingPlaced.class, (EventSubscriber<BookingPlaced>) Require.requireDependency(handler, "handler")::accept);
    }

    public void onBookingResult(Consumer<BookingResultEvents> handler) {
        var subscriber = (EventSubscriber<BookingResultEvents>) Require.requireDependency(handler, "handler")::accept;
        eventBus.subscribe(BookingResultEvents.BookingConfirmed.class, subscriber);
        eventBus.subscribe(BookingResultEvents.BookingRefused.class, subscriber);
        eventBus.subscribe(BookingResultEvents.BookingRejected.class, subscriber);
    }

    public void onBookingResultIntegration(Consumer<BookingResultIntegrationEvent> handler) {
        var guarded = Require.requireDependency(handler, "handler");
        eventBus.subscribe(BookingResultEvents.BookingConfirmed.class, (EventSubscriber<BookingResultEvents.BookingConfirmed>) event ->
            guarded.accept(new BookingCompletedIntegrationEvent(event.giftCardReference(), event.amount())));
        eventBus.subscribe(BookingResultEvents.BookingRefused.class, (EventSubscriber<BookingResultEvents.BookingRefused>) event ->
            guarded.accept(new BookingRefusedIntegrationEvent(event.giftCardReference(), event.amount())));
    }

    public void onBookingRejectedIntegration(Consumer<BookingRejectedIntegrationEvent> handler) {
        var guarded = Require.requireDependency(handler, "handler");
        eventBus.subscribe(BookingResultEvents.BookingRejected.class, (EventSubscriber<BookingResultEvents.BookingRejected>) event ->
            guarded.accept(new BookingRejectedIntegrationEvent(event.giftCardReference(), event.amount())));
    }

    public void configure(JavalinConfig config) {
        BookingPlacing bookingPlacing = new BookingPlacing(bookingRepository, eventBus);
        BookingQueryService bookingQueryService = new BookingQueryService(bookingRepository);

        BookingApi api = new BookingApi(bookingPlacing, bookingQueryService);
        api.configure(config);
    }

    private HandlePaymentResultFromPayment createHandlePaymentResultFromPayment() {
        PaymentResult paymentResult = new PaymentResult(bookingRepository);
        BookingConfirming bookingConfirming = new BookingConfirming(bookingRepository, eventBus);
        BookingRejecting bookingRejecting = new BookingRejecting(bookingRepository, eventBus);
        return new HandlePaymentResultFromPayment(paymentResult, bookingConfirming, bookingRejecting);
    }
}
