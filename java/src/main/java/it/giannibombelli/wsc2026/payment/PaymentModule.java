package it.giannibombelli.wsc2026.payment;

import it.giannibombelli.wsc2026.common.utils.Require;

import io.javalin.config.JavalinConfig;
import it.giannibombelli.wsc2026.common.application.events.EventBus;
import it.giannibombelli.wsc2026.common.application.events.EventSubscriber;
import it.giannibombelli.wsc2026.common.module.ApplicationModule;
import it.giannibombelli.wsc2026.payment.api.PaymentApi;
import it.giannibombelli.wsc2026.payment.api.PaymentInternalApi;
import it.giannibombelli.wsc2026.payment.application.query.PaymentFinder;
import it.giannibombelli.wsc2026.payment.application.services.PaymentProcessing;
import it.giannibombelli.wsc2026.payment.application.services.RefundHandling;
import it.giannibombelli.wsc2026.payment.application.usecases.*;
import it.giannibombelli.wsc2026.payment.domain.events.*;
import it.giannibombelli.wsc2026.payment.integration.PaymentResultIntegrationEvent;
import it.giannibombelli.wsc2026.payment.application.policies.PaymentCompletion;
import it.giannibombelli.wsc2026.payment.application.policies.PaymentExpiration;
import it.giannibombelli.wsc2026.payment.application.policies.PaymentRejection;
import it.giannibombelli.wsc2026.payment.application.policies.TransactionRefund;
import it.giannibombelli.wsc2026.payment.domain.ports.PaymentProvider;
import it.giannibombelli.wsc2026.payment.domain.ports.PaymentRepository;
import it.giannibombelli.wsc2026.payment.infrastructure.InMemoryPaymentEventBus;
import it.giannibombelli.wsc2026.payment.infrastructure.PaymentDeadlineWatcher;
import it.giannibombelli.wsc2026.payment.infrastructure.SqlitePaymentRepository;
import it.giannibombelli.wsc2026.payment.infrastructure.providers.GiftCard;
import it.giannibombelli.wsc2026.payment.infrastructure.providers.Klarna;
import it.giannibombelli.wsc2026.payment.infrastructure.providers.PayPal;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import static java.util.Objects.requireNonNull;

public final class PaymentModule extends ApplicationModule {

    private final DataSource dataSource;
    private final PaymentRepository paymentRepository;
    private final EventBus<PaymentEvent> eventBus;
    private final PaymentRequesting paymentRequesting;
    private final RefundRequesting refundRequesting;
    private final List<Consumer<PaymentResultEvents.PaymentAccepted>> acceptedHandlers;
    private final List<Consumer<PaymentResultEvents.PaymentRejected>> rejectedHandlers;
    private final List<Consumer<PaymentResultEvents.PaymentExpired>> expiredHandlers;
    private final List<Consumer<PaymentResultIntegrationEvent.PaymentAcceptedIntegrationEvent>> acceptedIntegrationHandlers;
    private final List<Consumer<PaymentResultIntegrationEvent.PaymentRejectedIntegrationEvent>> rejectedIntegrationHandlers;
    private final List<Consumer<PaymentResultIntegrationEvent.PaymentExpiredIntegrationEvent>> expiredIntegrationHandlers;
    private PaymentDeadlineWatcher watcher;

    public PaymentModule(DataSource dataSource) {
        this(dataSource, List.of(), List.of(), List.of());
    }

    public PaymentModule(DataSource dataSource,
                         List<Consumer<PaymentResultEvents.PaymentAccepted>> acceptedHandlers,
                         List<Consumer<PaymentResultEvents.PaymentRejected>> rejectedHandlers,
                         List<Consumer<PaymentResultEvents.PaymentExpired>> expiredHandlers) {
        super();
        Require.requireDependency(dataSource, "dataSource");
        this.dataSource = dataSource;
        this.paymentRepository = new SqlitePaymentRepository(dataSource);
        this.eventBus = new InMemoryPaymentEventBus(Executors.newVirtualThreadPerTaskExecutor());
        this.acceptedHandlers = new ArrayList<>(requireNonNull(acceptedHandlers));
        this.rejectedHandlers = new ArrayList<>(requireNonNull(rejectedHandlers));
        this.expiredHandlers = new ArrayList<>(requireNonNull(expiredHandlers));
        this.acceptedIntegrationHandlers = new ArrayList<>();
        this.rejectedIntegrationHandlers = new ArrayList<>();
        this.expiredIntegrationHandlers = new ArrayList<>();
        this.paymentRequesting = new PaymentRequesting(paymentRepository, eventBus);
        this.refundRequesting = new RefundRequesting(paymentRepository, eventBus);
        registerCrossBcResultHandlers();
    }

    private void registerCrossBcResultHandlers() {
        eventBus.subscribe(PaymentResultEvents.PaymentAccepted.class,
            (EventSubscriber<PaymentResultEvents.PaymentAccepted>) event -> {
                acceptedHandlers.forEach(h -> h.accept(event));
                PaymentResultIntegrationEvent.PaymentAcceptedIntegrationEvent integrationEvent =
                    new PaymentResultIntegrationEvent.PaymentAcceptedIntegrationEvent(
                        event.clientReference().value().toString(), event.amount());
                acceptedIntegrationHandlers.forEach(h -> h.accept(integrationEvent));
            });
        eventBus.subscribe(PaymentResultEvents.PaymentRejected.class,
            (EventSubscriber<PaymentResultEvents.PaymentRejected>) event -> {
                rejectedHandlers.forEach(h -> h.accept(event));
                PaymentResultIntegrationEvent.PaymentRejectedIntegrationEvent integrationEvent =
                    new PaymentResultIntegrationEvent.PaymentRejectedIntegrationEvent(
                        event.clientReference().value().toString(), event.amount(), event.reason().value());
                rejectedIntegrationHandlers.forEach(h -> h.accept(integrationEvent));
            });
        eventBus.subscribe(PaymentResultEvents.PaymentExpired.class,
            (EventSubscriber<PaymentResultEvents.PaymentExpired>) event -> {
                expiredHandlers.forEach(h -> h.accept(event));
                PaymentResultIntegrationEvent.PaymentExpiredIntegrationEvent integrationEvent =
                    new PaymentResultIntegrationEvent.PaymentExpiredIntegrationEvent(
                        event.clientReference().value().toString(), event.amount());
                expiredIntegrationHandlers.forEach(h -> h.accept(integrationEvent));
            });
    }

    public PaymentRepository paymentRepository() {
        return paymentRepository;
    }

    public PaymentRequesting paymentRequesting() {
        return paymentRequesting;
    }

    public RefundRequesting refundRequesting() {
        return refundRequesting;
    }

    public void addAcceptedHandler(Consumer<PaymentResultEvents.PaymentAccepted> handler) {
        acceptedHandlers.add(requireNonNull(handler));
    }

    public void addRejectedHandler(Consumer<PaymentResultEvents.PaymentRejected> handler) {
        rejectedHandlers.add(requireNonNull(handler));
    }

    public void addExpiredHandler(Consumer<PaymentResultEvents.PaymentExpired> handler) {
        expiredHandlers.add(requireNonNull(handler));
    }

    public void addAcceptedIntegrationHandler(Consumer<PaymentResultIntegrationEvent.PaymentAcceptedIntegrationEvent> handler) {
        acceptedIntegrationHandlers.add(requireNonNull(handler));
    }

    public void addRejectedIntegrationHandler(Consumer<PaymentResultIntegrationEvent.PaymentRejectedIntegrationEvent> handler) {
        rejectedIntegrationHandlers.add(requireNonNull(handler));
    }

    public void addExpiredIntegrationHandler(Consumer<PaymentResultIntegrationEvent.PaymentExpiredIntegrationEvent> handler) {
        expiredIntegrationHandlers.add(requireNonNull(handler));
    }

    public void configure(JavalinConfig config) {
        PaymentFinder paymentFinder = new PaymentFinder(paymentRepository);

        PaymentCompletion paymentCompletion = new PaymentCompletion();
        PaymentRejection paymentRejection = new PaymentRejection();
        PaymentExpiration paymentExpiration = new PaymentExpiration();
        TransactionRefund transactionRefund = new TransactionRefund();

        TransactionAccepting transactionAccepting = new TransactionAccepting(paymentRepository, eventBus, paymentCompletion);
        TransactionRejecting transactionRejecting = new TransactionRejecting(paymentRepository, eventBus, paymentRejection);
        PaymentExpiring paymentExpiring = new PaymentExpiring(paymentRepository, eventBus, paymentExpiration);

        eventBus.subscribe(TransactionAccepted.class, transactionAccepting);
        eventBus.subscribe(TransactionRejected.class, transactionRejecting);
        eventBus.subscribe(PaymentDeadlineReached.class, paymentExpiring);

        Map<String, PaymentProvider> providers = Map.of(
            "PayPal", new PayPal(),
            "Klarna", new Klarna(),
            "GiftCard", new GiftCard()
        );

        RefundHandling refundHandling = new RefundHandling(paymentRepository, providers, transactionRefund, eventBus);
        eventBus.subscribe(RefundRequested.class, refundHandling);

        PaymentProcessing paymentProcessing = new PaymentProcessing(paymentRepository, providers, eventBus);

        PaymentApi api = new PaymentApi(paymentFinder, paymentProcessing);
        api.configure(config);

        PaymentInternalApi internalApi = new PaymentInternalApi(paymentRequesting, paymentFinder);
        internalApi.configure(config);

        watcher = new PaymentDeadlineWatcher(paymentRepository, paymentExpiration, eventBus);
        watcher.start();
    }

    public void stop() {
        if (watcher != null) {
            watcher.stop();
        }
    }
}
