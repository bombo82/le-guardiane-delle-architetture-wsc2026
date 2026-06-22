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
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public final class BookingModule extends ApplicationModule {

    private final DataSource dataSource;
    private final SqliteBookingRepository bookingRepository;
    private final EventBus<BookingEvent> eventBus;
    private final HandlePaymentResultFromPayment handlePaymentResultFromPayment;
    private final List<Consumer<BookingPlaced>> bookingPlacedHandlers;
    private final List<Consumer<BookingResultEvents>> bookingResultHandlers;
    private final List<Consumer<BookingResultIntegrationEvent>> bookingResultIntegrationHandlers;
    private final List<Consumer<BookingRejectedIntegrationEvent>> bookingRejectedIntegrationHandlers;

    public BookingModule(DataSource dataSource) {
        super();
        Require.requireDependency(dataSource, "dataSource");

        this.dataSource = dataSource;
        this.bookingRepository = new SqliteBookingRepository(dataSource);
        this.eventBus = new InMemoryBookingEventBus(Runnable::run);
        this.bookingPlacedHandlers = new ArrayList<>();
        this.bookingResultHandlers = new ArrayList<>();
        this.bookingResultIntegrationHandlers = new ArrayList<>();
        this.bookingRejectedIntegrationHandlers = new ArrayList<>();
        this.handlePaymentResultFromPayment = createHandlePaymentResultFromPayment();
    }

    public HandlePaymentResultFromPayment handlePaymentResultFromPayment() {
        return handlePaymentResultFromPayment;
    }

    public void onBookingPlaced(Consumer<BookingPlaced> handler) {
        bookingPlacedHandlers.add(Require.requireDependency(handler, "handler"));
        eventBus.subscribe(BookingPlaced.class, (EventSubscriber<BookingPlaced>) handler::accept);
    }

    public void onBookingResult(Consumer<BookingResultEvents> handler) {
        bookingResultHandlers.add(Require.requireDependency(handler, "handler"));
        eventBus.subscribe(BookingResultEvents.BookingConfirmed.class, (EventSubscriber<BookingResultEvents.BookingConfirmed>) handler::accept);
        eventBus.subscribe(BookingResultEvents.BookingRefused.class, (EventSubscriber<BookingResultEvents.BookingRefused>) handler::accept);
        eventBus.subscribe(BookingResultEvents.BookingRejected.class, (EventSubscriber<BookingResultEvents.BookingRejected>) handler::accept);
    }

    public void onBookingResultIntegration(Consumer<BookingResultIntegrationEvent> handler) {
        bookingResultIntegrationHandlers.add(Require.requireDependency(handler, "handler"));
        eventBus.subscribe(BookingResultEvents.BookingConfirmed.class, (EventSubscriber<BookingResultEvents.BookingConfirmed>) event ->
            handler.accept(new BookingCompletedIntegrationEvent(event.giftCardReference(), event.amount())));
        eventBus.subscribe(BookingResultEvents.BookingRefused.class, (EventSubscriber<BookingResultEvents.BookingRefused>) event ->
            handler.accept(new BookingRefusedIntegrationEvent(event.giftCardReference(), event.amount())));
    }

    public void onBookingRejectedIntegration(Consumer<BookingRejectedIntegrationEvent> handler) {
        bookingRejectedIntegrationHandlers.add(Require.requireDependency(handler, "handler"));
        eventBus.subscribe(BookingResultEvents.BookingRejected.class, (EventSubscriber<BookingResultEvents.BookingRejected>) event ->
            handler.accept(new BookingRejectedIntegrationEvent(event.giftCardReference(), event.amount())));
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
