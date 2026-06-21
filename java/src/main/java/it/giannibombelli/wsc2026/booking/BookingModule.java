package it.giannibombelli.wsc2026.booking;

import it.giannibombelli.wsc2026.common.utils.Require;

import io.javalin.config.JavalinConfig;
import it.giannibombelli.wsc2026.booking.api.BookingApi;
import it.giannibombelli.wsc2026.booking.application.services.PaymentResultOutcome;
import it.giannibombelli.wsc2026.booking.application.usecases.BookingConfirming;
import it.giannibombelli.wsc2026.booking.application.usecases.BookingPlacing;
import it.giannibombelli.wsc2026.booking.application.usecases.BookingRejecting;
import it.giannibombelli.wsc2026.booking.domain.events.BookingEvent;
import it.giannibombelli.wsc2026.booking.domain.events.BookingPlaced;
import it.giannibombelli.wsc2026.booking.domain.events.BookingResultEvents;
import it.giannibombelli.wsc2026.booking.application.policies.PaymentPolicy;
import it.giannibombelli.wsc2026.booking.infrastructure.InMemoryBookingEventBus;
import it.giannibombelli.wsc2026.booking.infrastructure.SqliteBookingRepository;
import it.giannibombelli.wsc2026.common.application.events.EventBus;
import it.giannibombelli.wsc2026.common.application.events.EventSubscriber;
import it.giannibombelli.wsc2026.common.module.ApplicationModule;

import javax.sql.DataSource;
import java.util.List;
import java.util.function.Consumer;

public final class BookingModule extends ApplicationModule {

    private final DataSource dataSource;
    private final SqliteBookingRepository bookingRepository;
    private final EventBus<BookingEvent> eventBus;
    private final PaymentResultOutcome paymentResultOutcome;
    private final List<Consumer<BookingPlaced>> bookingPlacedHandlers;
    private final List<Consumer<BookingResultEvents>> bookingConfirmedHandlers;
    private final List<Consumer<BookingResultEvents.BookingRejected>> bookingRejectedHandlers;

    public BookingModule(DataSource dataSource) {
        this(dataSource, List.of(), List.of(), List.of());
    }

    public BookingModule(DataSource dataSource,
                         List<Consumer<BookingPlaced>> bookingPlacedHandlers,
                         List<Consumer<BookingResultEvents>> bookingConfirmedHandlers,
                         List<Consumer<BookingResultEvents.BookingRejected>> bookingRejectedHandlers) {
        super();
        Require.requireDependency(dataSource, "dataSource");

        this.dataSource = dataSource;
        this.bookingRepository = new SqliteBookingRepository(dataSource);
        this.eventBus = new InMemoryBookingEventBus(Runnable::run);
        this.bookingPlacedHandlers = bookingPlacedHandlers;
        this.bookingConfirmedHandlers = bookingConfirmedHandlers;
        this.bookingRejectedHandlers = bookingRejectedHandlers;
        registerCrossBcHandlers();
        paymentResultOutcome = createPaymentResultOutcome();
    }

    public PaymentResultOutcome paymentResultOutcome() {
        return paymentResultOutcome;
    }

    public void configure(JavalinConfig config) {
        BookingPlacing bookingPlacing = new BookingPlacing(bookingRepository, eventBus);

        BookingApi api = new BookingApi(bookingPlacing, bookingRepository);
        api.configure(config);
    }

    private PaymentResultOutcome createPaymentResultOutcome() {
        PaymentPolicy paymentPolicy = new PaymentPolicy(bookingRepository);
        BookingConfirming bookingConfirming = new BookingConfirming(bookingRepository, eventBus);
        BookingRejecting bookingRejecting = new BookingRejecting(bookingRepository, eventBus);
        return new PaymentResultOutcome(paymentPolicy, bookingConfirming, bookingRejecting);
    }

    private void registerCrossBcHandlers() {
        bookingPlacedHandlers.forEach(handler ->
            eventBus.subscribe(BookingPlaced.class, (EventSubscriber<BookingPlaced>) handler::accept));
        bookingConfirmedHandlers.forEach(handler -> {
            eventBus.subscribe(BookingResultEvents.BookingConfirmed.class, (EventSubscriber<BookingResultEvents.BookingConfirmed>) handler::accept);
            eventBus.subscribe(BookingResultEvents.BookingRefused.class, (EventSubscriber<BookingResultEvents.BookingRefused>) handler::accept);
        });
        bookingRejectedHandlers.forEach(handler ->
            eventBus.subscribe(BookingResultEvents.BookingRejected.class, (EventSubscriber<BookingResultEvents.BookingRejected>) handler::accept));
    }
}
