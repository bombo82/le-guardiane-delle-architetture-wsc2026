package it.giannibombelli.wsc2026.booking;

import it.giannibombelli.wsc2026.booking.api.BookingApi;
import it.giannibombelli.wsc2026.booking.application.integration.payment.adapter.PaymentRequest;
import it.giannibombelli.wsc2026.booking.application.integration.payment.adapter.PaymentResult;
import it.giannibombelli.wsc2026.booking.application.integration.payment.adapter.RefundRequest;
import it.giannibombelli.wsc2026.booking.application.integration.payment.handlers.HandlePaymentResultFromPayment;
import it.giannibombelli.wsc2026.booking.application.query.BookingQueryService;
import it.giannibombelli.wsc2026.booking.application.usecases.BookingConfirming;
import it.giannibombelli.wsc2026.booking.application.usecases.BookingPlacing;
import it.giannibombelli.wsc2026.booking.application.usecases.BookingRejecting;
import it.giannibombelli.wsc2026.booking.domain.events.BookingEvent;
import it.giannibombelli.wsc2026.booking.domain.events.BookingPlaced;
import it.giannibombelli.wsc2026.booking.domain.events.BookingResultEvents;
import it.giannibombelli.wsc2026.booking.domain.ports.BookingRepository;
import it.giannibombelli.wsc2026.booking.infrastructure.InMemoryBookingEventBus;
import it.giannibombelli.wsc2026.booking.infrastructure.SqliteBookingRepository;
import it.giannibombelli.wsc2026.booking.integration.giftcard.BookingResultIntegrationEvent;
import it.giannibombelli.wsc2026.common.application.events.EventBus;
import it.giannibombelli.wsc2026.common.application.events.EventSubscriber;
import it.giannibombelli.wsc2026.common.module.ApplicationModule;
import it.giannibombelli.wsc2026.common.module.WebApi;
import it.giannibombelli.wsc2026.common.utils.Require;
import it.giannibombelli.wsc2026.payment.integration.PaymentRequestIntegrationCommand;
import it.giannibombelli.wsc2026.payment.integration.PaymentResultIntegrationEvent;
import it.giannibombelli.wsc2026.payment.integration.RefundRequestIntegrationCommand;

import javax.sql.DataSource;
import java.util.List;
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

    @Override
    public List<WebApi> webApis() {
        BookingPlacing bookingPlacing = new BookingPlacing(bookingRepository, eventBus);
        BookingQueryService bookingQueryService = new BookingQueryService(bookingRepository);

        return List.of(new BookingApi(bookingPlacing, bookingQueryService));
    }

    public void handlePaymentResult(PaymentResultIntegrationEvent event) {
        Require.requireArgument(event, "event");
        handlePaymentResultFromPayment.handle(event);
    }

    public void onBookingPlaced(Consumer<PaymentRequestIntegrationCommand> handler) {
        var guarded = Require.requireDependency(handler, "handler");
        eventBus.subscribe(BookingPlaced.class,
            (EventSubscriber<BookingPlaced>) event ->
                guarded.accept(PaymentRequest.fromBookingPlaced(event)));
    }

    public void onBookingRefused(Consumer<RefundRequestIntegrationCommand> handler) {
        var guarded = Require.requireDependency(handler, "handler");
        eventBus.subscribe(BookingResultEvents.BookingRefused.class,
            (EventSubscriber<BookingResultEvents.BookingRefused>) event ->
                guarded.accept(RefundRequest.fromBookingRefused(event)));
    }

    public void onBookingCompletedIntegration(Consumer<BookingResultIntegrationEvent.BookingCompletedIntegrationEvent> handler) {
        var guarded = Require.requireDependency(handler, "handler");
        eventBus.subscribe(BookingResultEvents.BookingConfirmed.class,
            (EventSubscriber<BookingResultEvents.BookingConfirmed>) event ->
                guarded.accept(new BookingResultIntegrationEvent.BookingCompletedIntegrationEvent(event.giftCardReference(), event.amount())));
    }

    public void onBookingRefusedIntegration(Consumer<BookingResultIntegrationEvent.BookingRefusedIntegrationEvent> handler) {
        var guarded = Require.requireDependency(handler, "handler");
        eventBus.subscribe(BookingResultEvents.BookingRefused.class,
            (EventSubscriber<BookingResultEvents.BookingRefused>) event ->
                guarded.accept(new BookingResultIntegrationEvent.BookingRefusedIntegrationEvent(event.giftCardReference(), event.amount())));
    }

    public void onBookingRejectedIntegration(Consumer<BookingResultIntegrationEvent.BookingRejectedIntegrationEvent> handler) {
        var guarded = Require.requireDependency(handler, "handler");
        eventBus.subscribe(BookingResultEvents.BookingRejected.class,
            (EventSubscriber<BookingResultEvents.BookingRejected>) event ->
                guarded.accept(new BookingResultIntegrationEvent.BookingRejectedIntegrationEvent(event.giftCardReference(), event.amount())));
    }

    private HandlePaymentResultFromPayment createHandlePaymentResultFromPayment() {
        PaymentResult paymentResult = new PaymentResult(bookingRepository);
        BookingConfirming bookingConfirming = new BookingConfirming(bookingRepository, eventBus);
        BookingRejecting bookingRejecting = new BookingRejecting(bookingRepository, eventBus);
        return new HandlePaymentResultFromPayment(paymentResult, bookingConfirming, bookingRejecting);
    }
}
