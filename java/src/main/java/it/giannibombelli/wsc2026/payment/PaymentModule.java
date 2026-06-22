package it.giannibombelli.wsc2026.payment;

import it.giannibombelli.wsc2026.common.utils.Require;

import io.javalin.config.JavalinConfig;
import it.giannibombelli.wsc2026.common.application.events.EventBus;
import it.giannibombelli.wsc2026.common.application.events.EventSubscriber;
import it.giannibombelli.wsc2026.common.domain.identity.EntityId;
import it.giannibombelli.wsc2026.common.domain.primitive.ClientReference;
import it.giannibombelli.wsc2026.common.domain.primitive.Timestamp;
import it.giannibombelli.wsc2026.common.module.ApplicationModule;
import it.giannibombelli.wsc2026.payment.api.PaymentApi;
import it.giannibombelli.wsc2026.payment.api.PaymentInternalApi;
import it.giannibombelli.wsc2026.payment.application.query.PaymentFinder;
import it.giannibombelli.wsc2026.payment.application.services.PaymentProcessing;
import it.giannibombelli.wsc2026.payment.application.services.RefundHandling;
import it.giannibombelli.wsc2026.payment.application.usecases.*;
import it.giannibombelli.wsc2026.payment.domain.events.*;
import it.giannibombelli.wsc2026.payment.domain.payment.PaymentId;
import it.giannibombelli.wsc2026.payment.integration.PaymentRequestIntegrationCommand;
import it.giannibombelli.wsc2026.payment.integration.PaymentResultIntegrationEvent;
import it.giannibombelli.wsc2026.payment.integration.RefundRequestIntegrationCommand;
import it.giannibombelli.wsc2026.payment.application.commands.RefundTransaction;
import it.giannibombelli.wsc2026.payment.application.commands.RequestPayment;
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
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public final class PaymentModule extends ApplicationModule {

    private final PaymentRepository paymentRepository;
    private final EventBus<PaymentEvent> eventBus;
    private final PaymentRequesting paymentRequesting;
    private final RefundRequesting refundRequesting;
    private final List<Consumer<PaymentResultIntegrationEvent>> paymentResultIntegrationHandlers;
    private PaymentDeadlineWatcher watcher;

    public PaymentModule(DataSource dataSource) {
        Require.requireDependency(dataSource, "dataSource");
        this.paymentRepository = new SqlitePaymentRepository(dataSource);
        this.eventBus = new InMemoryPaymentEventBus(Executors.newVirtualThreadPerTaskExecutor());
        this.paymentResultIntegrationHandlers = new ArrayList<>();
        this.paymentRequesting = new PaymentRequesting(paymentRepository, eventBus);
        this.refundRequesting = new RefundRequesting(paymentRepository, eventBus);
        registerCrossBcResultHandlers();
    }

    private void registerCrossBcResultHandlers() {
        eventBus.subscribe(PaymentResultEvents.PaymentAccepted.class,
            (EventSubscriber<PaymentResultEvents.PaymentAccepted>) event -> {
                PaymentResultIntegrationEvent.PaymentAcceptedIntegrationEvent integrationEvent =
                    new PaymentResultIntegrationEvent.PaymentAcceptedIntegrationEvent(
                        event.clientReference().value().toString(), event.amount());
                paymentResultIntegrationHandlers.forEach(h -> h.accept(integrationEvent));
            });
        eventBus.subscribe(PaymentResultEvents.PaymentRejected.class,
            (EventSubscriber<PaymentResultEvents.PaymentRejected>) event -> {
                PaymentResultIntegrationEvent.PaymentRejectedIntegrationEvent integrationEvent =
                    new PaymentResultIntegrationEvent.PaymentRejectedIntegrationEvent(
                        event.clientReference().value().toString(), event.amount(), event.reason().value());
                paymentResultIntegrationHandlers.forEach(h -> h.accept(integrationEvent));
            });
        eventBus.subscribe(PaymentResultEvents.PaymentExpired.class,
            (EventSubscriber<PaymentResultEvents.PaymentExpired>) event -> {
                PaymentResultIntegrationEvent.PaymentExpiredIntegrationEvent integrationEvent =
                    new PaymentResultIntegrationEvent.PaymentExpiredIntegrationEvent(
                        event.clientReference().value().toString(), event.amount());
                paymentResultIntegrationHandlers.forEach(h -> h.accept(integrationEvent));
            });
    }

    public void onPaymentResult(Consumer<PaymentResultIntegrationEvent> handler) {
        paymentResultIntegrationHandlers.add(Require.requireDependency(handler, "handler"));
    }

    public void requestPayment(PaymentRequestIntegrationCommand command) {
        Require.requireArgument(command, "command");
        var internalCommand = new RequestPayment(
            EntityId.generate(PaymentId::new),
            new ClientReference(UUID.fromString(command.clientReference())),
            command.amount(),
            Timestamp.now()
        );
        paymentRequesting.invoke(internalCommand);
    }

    public void requestRefund(RefundRequestIntegrationCommand command) {
        Require.requireArgument(command, "command");
        var clientReference = new ClientReference(UUID.fromString(command.clientReference()));
        var payment = paymentRepository.findByClientReference(clientReference)
            .orElseThrow(() -> new IllegalStateException(
                "No payment found for clientReference: " + command.clientReference()));
        var internalCommand = new RefundTransaction(payment.id(), command.amount());
        refundRequesting.invoke(internalCommand);
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
