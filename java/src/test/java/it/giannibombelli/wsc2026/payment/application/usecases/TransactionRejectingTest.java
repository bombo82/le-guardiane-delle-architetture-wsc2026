package it.giannibombelli.wsc2026.payment.application.usecases;

import it.giannibombelli.wsc2026.common.domain.identity.EntityId;
import it.giannibombelli.wsc2026.common.domain.primitive.ClientReference;
import it.giannibombelli.wsc2026.common.domain.primitive.Description;
import it.giannibombelli.wsc2026.common.domain.primitive.Money;
import it.giannibombelli.wsc2026.common.domain.primitive.Timestamp;
import it.giannibombelli.wsc2026.payment.domain.events.PaymentEvent;
import it.giannibombelli.wsc2026.payment.domain.events.PaymentResultEvents;
import it.giannibombelli.wsc2026.payment.domain.events.TransactionRejected;
import it.giannibombelli.wsc2026.payment.domain.events.TransactionStarted;
import it.giannibombelli.wsc2026.payment.domain.payment.Payment;
import it.giannibombelli.wsc2026.payment.domain.payment.PaymentId;
import it.giannibombelli.wsc2026.payment.domain.payment.PaymentStatus;
import it.giannibombelli.wsc2026.payment.domain.payment.Provider;
import it.giannibombelli.wsc2026.payment.domain.payment.ProviderReference;
import it.giannibombelli.wsc2026.payment.domain.payment.TransactionId;
import it.giannibombelli.wsc2026.payment.domain.policies.PaymentRejection;
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

class TransactionRejectingTest {
    private PaymentRepository repository;
    private CapturingEventPublisher<PaymentEvent> publisher;
    private TransactionRejecting transactionRejecting;

    @BeforeEach
    void setUp() {
        final DataSource dataSource = DatabaseSetup.initializeInMemoryDb("payment");
        repository = new SqlitePaymentRepository(dataSource);
        publisher = new CapturingEventPublisher<>();
        transactionRejecting = new TransactionRejecting(repository, publisher, new PaymentRejection());
    }

    @Nested
    class Construction {
        @Test
        void rejectNullRepository() {
            assertThatThrownBy(() -> new TransactionRejecting(null, publisher, new PaymentRejection()))
                .isInstanceOf(NullPointerException.class);
        }

        @Test
        void rejectNullPublisher() {
            assertThatThrownBy(() -> new TransactionRejecting(repository, null, new PaymentRejection()))
                .isInstanceOf(NullPointerException.class);
        }

        @Test
        void rejectNullPolicy() {
            assertThatThrownBy(() -> new TransactionRejecting(repository, publisher, null))
                .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    class Execution {
        @Test
        void rejectNullEvent() {
            assertThatThrownBy(() -> transactionRejecting.on(null))
                .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void shouldRejectTransaction() {
            PaymentId paymentId = EntityId.generate(PaymentId::new);
            String clientReference = UUID.randomUUID().toString();
            Money amount = new Money(new BigDecimal("50.00"));
            Instant requestedAt = Instant.parse("2026-06-07T10:00:00Z");
            Payment payment = Payment.request(paymentId, new ClientReference(clientReference), amount, new Timestamp(requestedAt));
            repository.save(payment);

            TransactionId transactionId = startTransaction(paymentId, amount);

            transactionRejecting.on(new TransactionRejected(paymentId, Provider.PAYPAL, transactionId, new Description("declined")));

            Optional<Payment> loaded = repository.findById(paymentId);
            assertThat(loaded).isPresent();
            assertThat(loaded.get().status()).isEqualTo(PaymentStatus.REJECTED);
        }

        @Test
        void shouldPublishRejectedEvent() {
            PaymentId paymentId = EntityId.generate(PaymentId::new);
            String clientReference = UUID.randomUUID().toString();
            Money amount = new Money(new BigDecimal("50.00"));
            Instant requestedAt = Instant.parse("2026-06-07T10:00:00Z");
            Payment payment = Payment.request(paymentId, new ClientReference(clientReference), amount, new Timestamp(requestedAt));
            repository.save(payment);

            TransactionId transactionId = startTransaction(paymentId, amount);

            transactionRejecting.on(new TransactionRejected(paymentId, Provider.PAYPAL, transactionId, new Description("declined")));

            assertThat(publisher.events()).hasSize(1);
            assertThat(publisher.events().get(0)).isInstanceOf(PaymentResultEvents.PaymentRejected.class);
            PaymentResultEvents.PaymentRejected rejected = (PaymentResultEvents.PaymentRejected) publisher.events().get(0);
            assertThat(rejected.aggregateId()).isEqualTo(paymentId);
            assertThat(rejected.clientReference().value()).isEqualTo(clientReference);
        }

        @Test
        void shouldFailIfPaymentNotFound() {
            PaymentId paymentId = EntityId.generate(PaymentId::new);
            TransactionId transactionId = new TransactionId(UUID.randomUUID());

            assertThatThrownBy(() -> transactionRejecting.on(
                new TransactionRejected(paymentId, Provider.PAYPAL, transactionId, new Description("declined"))))
                .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void shouldFailIfAlreadyAccepted() {
            PaymentId paymentId = EntityId.generate(PaymentId::new);
            String clientReference = UUID.randomUUID().toString();
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
            assertThatThrownBy(() -> transactionRejecting.on(
                new TransactionRejected(paymentId, Provider.PAYPAL, transactionId, new Description("declined"))))
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
