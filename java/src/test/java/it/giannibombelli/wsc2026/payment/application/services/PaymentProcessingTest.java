package it.giannibombelli.wsc2026.payment.application.services;

import it.giannibombelli.wsc2026.common.application.events.EventBus;
import it.giannibombelli.wsc2026.common.domain.identity.EntityId;
import it.giannibombelli.wsc2026.common.domain.primitive.ClientReference;
import it.giannibombelli.wsc2026.common.domain.primitive.Description;
import it.giannibombelli.wsc2026.common.domain.primitive.Money;
import it.giannibombelli.wsc2026.common.domain.primitive.Timestamp;
import it.giannibombelli.wsc2026.payment.application.commands.StartTransaction;
import it.giannibombelli.wsc2026.payment.application.usecases.TransactionAccepting;
import it.giannibombelli.wsc2026.payment.application.usecases.TransactionRejecting;
import it.giannibombelli.wsc2026.payment.domain.events.PaymentEvent;
import it.giannibombelli.wsc2026.payment.domain.events.PaymentResultEvents;
import it.giannibombelli.wsc2026.payment.domain.events.TransactionAccepted;
import it.giannibombelli.wsc2026.payment.domain.events.TransactionRejected;
import it.giannibombelli.wsc2026.payment.domain.payment.Payment;
import it.giannibombelli.wsc2026.payment.domain.payment.PaymentId;
import it.giannibombelli.wsc2026.payment.domain.payment.PaymentStatus;
import it.giannibombelli.wsc2026.payment.domain.payment.Provider;
import it.giannibombelli.wsc2026.payment.domain.payment.ProviderReference;
import it.giannibombelli.wsc2026.payment.domain.policies.PaymentCompletion;
import it.giannibombelli.wsc2026.payment.domain.policies.PaymentRejection;
import it.giannibombelli.wsc2026.payment.domain.ports.PaymentProvider;
import it.giannibombelli.wsc2026.payment.domain.ports.PaymentProviderResult;
import it.giannibombelli.wsc2026.payment.domain.ports.PaymentRepository;
import it.giannibombelli.wsc2026.payment.infrastructure.InMemoryPaymentEventBus;
import it.giannibombelli.wsc2026.payment.infrastructure.SqlitePaymentRepository;
import it.giannibombelli.wsc2026.testsupport.DatabaseSetup;
import it.giannibombelli.wsc2026.testsupport.events.CapturingSubscriber;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentProcessingTest {
    private PaymentRepository repository;
    private EventBus<PaymentEvent> eventBus;
    private PaymentProcessing paymentProcessing;

    @BeforeEach
    void setUp() {
        final DataSource dataSource = DatabaseSetup.initializeInMemoryDb("payment");
        repository = new SqlitePaymentRepository(dataSource);
        eventBus = new InMemoryPaymentEventBus(Runnable::run);
        TransactionAccepting accepting = new TransactionAccepting(repository, eventBus, new PaymentCompletion());
        TransactionRejecting rejecting = new TransactionRejecting(repository, eventBus, new PaymentRejection());
        eventBus.subscribe(TransactionAccepted.class, accepting);
        eventBus.subscribe(TransactionRejected.class, rejecting);

        Timestamp providerAt = new Timestamp(Instant.parse("2026-06-07T10:30:00Z"));
        PaymentProvider alwaysSuccess = new PaymentProvider() {
            @Override
            public PaymentProviderResult process(UUID paymentId, UUID providerReference, Money amount) {
                return new PaymentProviderResult.Success(UUID.randomUUID(), providerAt);
            }

            @Override
            public PaymentProviderResult refund(UUID paymentId, UUID providerReference, Money amount) {
                return new PaymentProviderResult.Success(UUID.randomUUID(), Timestamp.now());
            }
        };
        PaymentProvider alwaysFailure = new PaymentProvider() {
            @Override
            public PaymentProviderResult process(UUID paymentId, UUID providerReference, Money amount) {
                return new PaymentProviderResult.Failure(new Description("declined"));
            }

            @Override
            public PaymentProviderResult refund(UUID paymentId, UUID providerReference, Money amount) {
                return new PaymentProviderResult.Failure(new Description("declined"));
            }
        };

        Map<String, PaymentProvider> providers = Map.of(
            "PayPal", alwaysSuccess,
            "Klarna", alwaysFailure
        );

        paymentProcessing = new PaymentProcessing(repository, providers, eventBus);
    }

    @Nested
    class Construction {
        @Test
        void rejectNullParameters() {
            Map<String, PaymentProvider> providers = Map.of();

            assertThatThrownBy(() -> new PaymentProcessing(null, providers, eventBus))
                .isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new PaymentProcessing(repository, null, eventBus))
                .isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new PaymentProcessing(repository, providers, null))
                .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    class Execution {
        @Test
        void rejectNullCommand() {
            assertThatThrownBy(() -> paymentProcessing.invoke(null))
                .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void shouldAcceptWhenProviderSucceeds() {
            PaymentId paymentId = EntityId.generate(PaymentId::new);
            String clientReference = UUID.randomUUID().toString();
            Money amount = new Money(new BigDecimal("50.00"));
            Instant requestedAt = Instant.parse("2026-06-07T10:00:00Z");
            Payment payment = Payment.request(paymentId, new ClientReference(clientReference), amount, new Timestamp(requestedAt));
            repository.save(payment);

            CapturingSubscriber<PaymentResultEvents.PaymentAccepted> acceptedCaptor = new CapturingSubscriber<>();
            eventBus.subscribe(PaymentResultEvents.PaymentAccepted.class, acceptedCaptor);

            paymentProcessing.invoke(
                new StartTransaction(paymentId, Provider.PAYPAL, new ProviderReference(UUID.randomUUID()), amount, Timestamp.now()));

            Optional<Payment> loaded = repository.findById(paymentId);
            assertThat(loaded).isPresent();
            assertThat(loaded.get().status()).isEqualTo(PaymentStatus.ACCEPTED);
            assertThat(acceptedCaptor.events()).hasSize(1);
        }

        @Test
        void shouldRejectWhenProviderFails() {
            PaymentId paymentId = EntityId.generate(PaymentId::new);
            String clientReference = UUID.randomUUID().toString();
            Money amount = new Money(new BigDecimal("50.00"));
            Instant requestedAt = Instant.parse("2026-06-07T10:00:00Z");
            Payment payment = Payment.request(paymentId, new ClientReference(clientReference), amount, new Timestamp(requestedAt));
            repository.save(payment);

            CapturingSubscriber<PaymentResultEvents.PaymentRejected> rejectedCaptor = new CapturingSubscriber<>();
            eventBus.subscribe(PaymentResultEvents.PaymentRejected.class, rejectedCaptor);

            paymentProcessing.invoke(
                new StartTransaction(paymentId, Provider.KLARNA, new ProviderReference(UUID.randomUUID()), amount, Timestamp.now()));

            Optional<Payment> loaded = repository.findById(paymentId);
            assertThat(loaded).isPresent();
            assertThat(loaded.get().status()).isEqualTo(PaymentStatus.REJECTED);
            assertThat(rejectedCaptor.events()).hasSize(1);
        }

        @Test
        void shouldFailIfPaymentNotFound() {
            PaymentId paymentId = EntityId.generate(PaymentId::new);
            Money amount = new Money(new BigDecimal("50.00"));

            assertThatThrownBy(() -> paymentProcessing.invoke(
                new StartTransaction(paymentId, Provider.PAYPAL, new ProviderReference(UUID.randomUUID()), amount, Timestamp.now())))
                .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void shouldFailIfProviderUnknown() {
            PaymentId paymentId = EntityId.generate(PaymentId::new);
            String clientReference = UUID.randomUUID().toString();
            Money amount = new Money(new BigDecimal("50.00"));
            Instant requestedAt = Instant.parse("2026-06-07T10:00:00Z");
            Payment payment = Payment.request(paymentId, new ClientReference(clientReference), amount, new Timestamp(requestedAt));
            repository.save(payment);

            assertThatThrownBy(() -> paymentProcessing.invoke(
                new StartTransaction(paymentId, Provider.fromLabel("UnknownProvider"), new ProviderReference(UUID.randomUUID()), amount, Timestamp.now())))
                .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
