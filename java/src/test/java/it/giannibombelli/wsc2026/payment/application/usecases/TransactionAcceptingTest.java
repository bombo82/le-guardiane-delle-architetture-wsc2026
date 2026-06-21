package it.giannibombelli.wsc2026.payment.application.usecases;

import it.giannibombelli.wsc2026.common.domain.identity.EntityId;
import it.giannibombelli.wsc2026.common.domain.primitive.ClientReference;
import it.giannibombelli.wsc2026.common.domain.primitive.Money;
import it.giannibombelli.wsc2026.common.domain.primitive.Timestamp;
import it.giannibombelli.wsc2026.payment.domain.events.PaymentEvent;
import it.giannibombelli.wsc2026.payment.domain.events.PaymentResultEvents;
import it.giannibombelli.wsc2026.payment.domain.events.TransactionAccepted;
import it.giannibombelli.wsc2026.payment.domain.events.TransactionStarted;
import it.giannibombelli.wsc2026.payment.domain.payment.Payment;
import it.giannibombelli.wsc2026.payment.domain.payment.PaymentId;
import it.giannibombelli.wsc2026.payment.domain.payment.PaymentStatus;
import it.giannibombelli.wsc2026.payment.domain.payment.Provider;
import it.giannibombelli.wsc2026.payment.domain.payment.ProviderReference;
import it.giannibombelli.wsc2026.payment.domain.payment.TransactionId;
import it.giannibombelli.wsc2026.payment.domain.policies.PaymentCompletion;
import it.giannibombelli.wsc2026.payment.domain.ports.PaymentRepository;
import it.giannibombelli.wsc2026.payment.infrastructure.SqlitePaymentRepository;
import it.giannibombelli.wsc2026.testsupport.DatabaseSetup;
import it.giannibombelli.wsc2026.testsupport.events.CapturingEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TransactionAcceptingTest {
    private PaymentRepository repository;
    private CapturingEventPublisher<PaymentEvent> publisher;
    private TransactionAccepting transactionAccepting;

    @BeforeEach
    void setUp() {
        final DataSource dataSource = DatabaseSetup.initializeInMemoryDb("payment");
        repository = new SqlitePaymentRepository(dataSource);
        publisher = new CapturingEventPublisher<>();
        transactionAccepting = new TransactionAccepting(repository, publisher, new PaymentCompletion());
    }

    @Nested
    class Construction {
        @Test
        void rejectNullRepository() {
            assertThatThrownBy(() -> new TransactionAccepting(null, publisher, new PaymentCompletion()))
                .isInstanceOf(NullPointerException.class);
        }

        @Test
        void rejectNullPublisher() {
            assertThatThrownBy(() -> new TransactionAccepting(repository, null, new PaymentCompletion()))
                .isInstanceOf(NullPointerException.class);
        }

        @Test
        void rejectNullPolicy() {
            assertThatThrownBy(() -> new TransactionAccepting(repository, publisher, null))
                .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    class Execution {
        @Test
        void rejectNullEvent() {
            assertThatThrownBy(() -> transactionAccepting.on(null))
                .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void shouldAcceptTransaction() {
            PaymentId paymentId = EntityId.generate(PaymentId::new);
            UUID clientReference = UUID.randomUUID();
            Money amount = new Money(new BigDecimal("50.00"));
            Instant requestedAt = Instant.parse("2026-06-07T10:00:00Z");
            Timestamp providerAt = new Timestamp(Instant.parse("2026-06-07T10:30:00Z"));
            Payment payment = Payment.request(paymentId, new ClientReference(clientReference), amount, new Timestamp(requestedAt));
            repository.save(payment);

            TransactionId transactionId = startTransaction(paymentId, amount);

            transactionAccepting.on(new TransactionAccepted(paymentId, Provider.PAYPAL, transactionId, amount, providerAt));

            Optional<Payment> loaded = repository.findById(paymentId);
            assertThat(loaded).isPresent();
            assertThat(loaded.get().status()).isEqualTo(PaymentStatus.ACCEPTED);
        }

        @Test
        void shouldPublishAcceptedEvent() {
            PaymentId paymentId = EntityId.generate(PaymentId::new);
            UUID clientReference = UUID.randomUUID();
            Money amount = new Money(new BigDecimal("50.00"));
            Instant requestedAt = Instant.parse("2026-06-07T10:00:00Z");
            Timestamp providerAt = new Timestamp(Instant.parse("2026-06-07T10:30:00Z"));
            Payment payment = Payment.request(paymentId, new ClientReference(clientReference), amount, new Timestamp(requestedAt));
            repository.save(payment);

            TransactionId transactionId = startTransaction(paymentId, amount);

            transactionAccepting.on(new TransactionAccepted(paymentId, Provider.PAYPAL, transactionId, amount, providerAt));

            assertThat(publisher.events()).hasSize(1);
            assertThat(publisher.events().get(0)).isInstanceOf(PaymentResultEvents.PaymentAccepted.class);
            PaymentResultEvents.PaymentAccepted accepted = (PaymentResultEvents.PaymentAccepted) publisher.events().get(0);
            assertThat(accepted.aggregateId()).isEqualTo(paymentId);
            assertThat(accepted.clientReference().value()).isEqualTo(clientReference);
        }

        @Test
        void shouldFailIfPaymentNotFound() {
            PaymentId paymentId = EntityId.generate(PaymentId::new);
            TransactionId transactionId = new TransactionId(UUID.randomUUID());
            Timestamp providerAt = new Timestamp(Instant.parse("2026-06-07T10:30:00Z"));

            assertThatThrownBy(() -> transactionAccepting.on(
                new TransactionAccepted(paymentId, Provider.PAYPAL, transactionId, new Money(new BigDecimal("10.00")), providerAt)))
                .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void shouldFailIfAlreadyAccepted() {
            PaymentId paymentId = EntityId.generate(PaymentId::new);
            UUID clientReference = UUID.randomUUID();
            Money amount = new Money(new BigDecimal("50.00"));
            Instant requestedAt = Instant.parse("2026-06-07T10:00:00Z");
            Payment payment = Payment.request(paymentId, new ClientReference(clientReference), amount, new Timestamp(requestedAt));
            TransactionId acceptedTransactionId = EntityId.generate(TransactionId::new);
            payment.startTransaction(
                acceptedTransactionId,
                Provider.PAYPAL,
                new ProviderReference(UUID.randomUUID()),
                amount,
                new Timestamp(requestedAt.plusSeconds(1))
            );
            payment.acceptTransaction(acceptedTransactionId, new Timestamp(requestedAt.plusSeconds(60)));
            repository.save(payment);

            TransactionId transactionId = new TransactionId(UUID.randomUUID());
            Timestamp providerAt = new Timestamp(Instant.parse("2026-06-07T10:30:00Z"));
            assertThatThrownBy(() -> transactionAccepting.on(
                new TransactionAccepted(paymentId, Provider.PAYPAL, transactionId, amount, providerAt)))
                .isInstanceOf(IllegalStateException.class);
        }

        private TransactionId startTransaction(PaymentId paymentId, Money amount) {
            Payment payment = repository.findById(paymentId).orElseThrow();
            TransactionId transactionId = EntityId.generate(TransactionId::new);
            TransactionStarted started = payment.startTransaction(
                transactionId,
                Provider.PAYPAL,
                new ProviderReference(UUID.randomUUID()),
                amount,
                new Timestamp(Instant.now())
            );
            repository.save(payment);
            return started.transactionId();
        }
    }
}
