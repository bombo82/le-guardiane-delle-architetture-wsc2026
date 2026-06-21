package it.giannibombelli.wsc2026.payment.application.services;

import it.giannibombelli.wsc2026.common.domain.identity.EntityId;
import it.giannibombelli.wsc2026.common.domain.primitive.ClientReference;
import it.giannibombelli.wsc2026.common.domain.primitive.Description;
import it.giannibombelli.wsc2026.common.domain.primitive.Money;
import it.giannibombelli.wsc2026.common.domain.primitive.Timestamp;
import it.giannibombelli.wsc2026.payment.domain.events.PaymentEvent;
import it.giannibombelli.wsc2026.payment.domain.events.RefundRequested;
import it.giannibombelli.wsc2026.payment.domain.events.RefundResultEvents;
import it.giannibombelli.wsc2026.payment.domain.payment.Payment;
import it.giannibombelli.wsc2026.payment.domain.payment.PaymentId;
import it.giannibombelli.wsc2026.payment.domain.payment.PaymentStatus;
import it.giannibombelli.wsc2026.payment.domain.payment.Provider;
import it.giannibombelli.wsc2026.payment.domain.payment.ProviderReference;
import it.giannibombelli.wsc2026.payment.domain.payment.TransactionId;
import it.giannibombelli.wsc2026.payment.domain.payment.TransactionStatus;
import it.giannibombelli.wsc2026.payment.application.policies.TransactionRefund;
import it.giannibombelli.wsc2026.payment.domain.ports.PaymentProvider;
import it.giannibombelli.wsc2026.payment.domain.ports.PaymentProviderResult;
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
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RefundHandlingTest {
    private PaymentRepository repository;
    private CapturingEventPublisher<PaymentEvent> publisher;
    private RefundHandling refundHandling;

    @BeforeEach
    void setUp() {
        final DataSource dataSource = DatabaseSetup.initializeInMemoryDb("payment");
        repository = new SqlitePaymentRepository(dataSource);
        publisher = new CapturingEventPublisher<>();

        PaymentProvider alwaysSuccess = new PaymentProvider() {
            @Override
            public PaymentProviderResult process(UUID paymentId, UUID providerReference, Money amount) {
                return new PaymentProviderResult.Success(UUID.randomUUID(), Timestamp.now());
            }

            @Override
            public PaymentProviderResult refund(UUID paymentId, UUID providerReference, Money amount) {
                return new PaymentProviderResult.Success(UUID.randomUUID(), Timestamp.now());
            }
        };
        PaymentProvider alwaysFailure = new PaymentProvider() {
            @Override
            public PaymentProviderResult process(UUID paymentId, UUID providerReference, Money amount) {
                return new PaymentProviderResult.Success(UUID.randomUUID(), Timestamp.now());
            }

            @Override
            public PaymentProviderResult refund(UUID paymentId, UUID providerReference, Money amount) {
                return new PaymentProviderResult.Failure(new Description("refund declined"));
            }
        };

        Map<String, PaymentProvider> providers = Map.of(
            "PayPal", alwaysSuccess,
            "Klarna", alwaysFailure,
            "GiftCard", alwaysSuccess
        );

        refundHandling = new RefundHandling(repository, providers, new TransactionRefund(), publisher);
    }

    @Nested
    class Construction {
        @Test
        void rejectNullRepository() {
            assertThatThrownBy(() -> new RefundHandling(null, Map.of(), new TransactionRefund(), publisher))
                .isInstanceOf(NullPointerException.class);
        }

        @Test
        void rejectNullProviders() {
            PaymentId paymentId = EntityId.generate(PaymentId::new);
            assertThatThrownBy(() -> new RefundHandling(repository, null, new TransactionRefund(), publisher))
                .isInstanceOf(NullPointerException.class);
        }

        @Test
        void rejectNullPolicy() {
            assertThatThrownBy(() -> new RefundHandling(repository, Map.of(), null, publisher))
                .isInstanceOf(NullPointerException.class);
        }

        @Test
        void rejectNullPublisher() {
            assertThatThrownBy(() -> new RefundHandling(repository, Map.of(), new TransactionRefund(), null))
                .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    class Execution {
        @Test
        void rejectNullEvent() {
            assertThatThrownBy(() -> refundHandling.on(null))
                .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void shouldRefundWhenProviderSucceeds() {
            PaymentId paymentId = seedAcceptedPayment("PayPal", new BigDecimal("50.00"));

            refundHandling.on(new RefundRequested(paymentId, new ClientReference(UUID.randomUUID()), new Money(new BigDecimal("50.00"))));

            Optional<Payment> loaded = repository.findById(paymentId);
            assertThat(loaded).isPresent();
            assertThat(loaded.get().status()).isEqualTo(PaymentStatus.REFUNDED);
            assertThat(loaded.get().transactions()).allMatch(t -> t.status() == TransactionStatus.REFUNDED);
            assertThat(publisher.events()).hasSize(1);
            assertThat(publisher.events().get(0)).isInstanceOf(RefundResultEvents.TransactionRefunded.class);
            RefundResultEvents.TransactionRefunded refunded = (RefundResultEvents.TransactionRefunded) publisher.events().get(0);
            assertThat(refunded.amount()).isEqualTo(new Money(new BigDecimal("50.00")));
        }

        @Test
        void shouldNotifyFailureWhenProviderRejects() {
            PaymentId paymentId = seedAcceptedPayment("Klarna", new BigDecimal("50.00"));

            refundHandling.on(new RefundRequested(paymentId, new ClientReference(UUID.randomUUID()), new Money(new BigDecimal("50.00"))));

            Optional<Payment> loaded = repository.findById(paymentId);
            assertThat(loaded).isPresent();
            assertThat(loaded.get().status()).isEqualTo(PaymentStatus.ACCEPTED);
            assertThat(publisher.events()).hasSize(1);
            assertThat(publisher.events().get(0)).isInstanceOf(RefundResultEvents.TransactionNotRefunded.class);
        }

        @Test
        void shouldRefundAllAcceptedTransactions() {
            PaymentId paymentId = seedSplitPayment(
                "PayPal", new BigDecimal("60.00"),
                "GiftCard", new BigDecimal("40.00")
            );

            refundHandling.on(new RefundRequested(paymentId, new ClientReference(UUID.randomUUID()), new Money(new BigDecimal("100.00"))));

            Optional<Payment> loaded = repository.findById(paymentId);
            assertThat(loaded).isPresent();
            assertThat(loaded.get().status()).isEqualTo(PaymentStatus.REFUNDED);
            assertThat(loaded.get().transactions()).allMatch(t -> t.status() == TransactionStatus.REFUNDED);
            assertThat(publisher.events()).hasSize(1);
            assertThat(publisher.events().get(0)).isInstanceOf(RefundResultEvents.TransactionRefunded.class);
            RefundResultEvents.TransactionRefunded refunded = (RefundResultEvents.TransactionRefunded) publisher.events().get(0);
            assertThat(refunded.amount()).isEqualTo(new Money(new BigDecimal("100.00")));
        }

        @Test
        void shouldMarkSuccessfulTransactionsAsRefundedEvenIfOneFails() {
            PaymentId paymentId = seedSplitPayment(
                "PayPal", new BigDecimal("60.00"),
                "Klarna", new BigDecimal("40.00")
            );

            refundHandling.on(new RefundRequested(paymentId, new ClientReference(UUID.randomUUID()), new Money(new BigDecimal("100.00"))));

            Optional<Payment> loaded = repository.findById(paymentId);
            assertThat(loaded).isPresent();
            Payment payment = loaded.get();
            assertThat(payment.status()).isEqualTo(PaymentStatus.ACCEPTED);
            long refundedCount = payment.transactions().stream().filter(t -> t.status() == TransactionStatus.REFUNDED).count();
            assertThat(refundedCount).isEqualTo(1);
            assertThat(publisher.events()).hasSize(1);
            assertThat(publisher.events().get(0)).isInstanceOf(RefundResultEvents.TransactionNotRefunded.class);
        }

        @Test
        void shouldFailIfPaymentNotFound() {
            PaymentId paymentId = EntityId.generate(PaymentId::new);

            assertThatThrownBy(() -> refundHandling.on(
                new RefundRequested(paymentId, new ClientReference(UUID.randomUUID()), new Money(new BigDecimal("10.00")))))
                .isInstanceOf(IllegalArgumentException.class);
        }

        private PaymentId seedAcceptedPayment(String provider, BigDecimal amount) {
            PaymentId paymentId = EntityId.generate(PaymentId::new);
            UUID clientReference = UUID.randomUUID();
            Money money = new Money(amount);
            Instant requestedAt = Instant.now();
            Payment payment = Payment.request(paymentId, new ClientReference(clientReference), money, new Timestamp(requestedAt));
            TransactionId transactionId = EntityId.generate(TransactionId::new);
            payment.startTransaction(
                transactionId,
                Provider.fromLabel(provider),
                new ProviderReference(UUID.randomUUID()),
                money,
                new Timestamp(requestedAt.plusSeconds(1))
            );
            payment.acceptTransaction(transactionId, new Timestamp(requestedAt.plusSeconds(60)));
            repository.save(payment);
            return paymentId;
        }

        private PaymentId seedSplitPayment(String firstProvider, BigDecimal firstAmount,
                                           String secondProvider, BigDecimal secondAmount) {
            PaymentId paymentId = EntityId.generate(PaymentId::new);
            UUID clientReference = UUID.randomUUID();
            Money total = new Money(firstAmount.add(secondAmount));
            Instant requestedAt = Instant.now();
            Payment payment = Payment.request(paymentId, new ClientReference(clientReference), total, new Timestamp(requestedAt));

            TransactionId firstTx = EntityId.generate(TransactionId::new);
            payment.startTransaction(
                firstTx,
                Provider.fromLabel(firstProvider),
                new ProviderReference(UUID.randomUUID()),
                new Money(firstAmount),
                new Timestamp(requestedAt.plusSeconds(1))
            );
            payment.acceptTransaction(firstTx, new Timestamp(requestedAt.plusSeconds(60)));

            TransactionId secondTx = EntityId.generate(TransactionId::new);
            payment.startTransaction(
                secondTx,
                Provider.fromLabel(secondProvider),
                new ProviderReference(UUID.randomUUID()),
                new Money(secondAmount),
                new Timestamp(requestedAt.plusSeconds(2))
            );
            payment.acceptTransaction(secondTx, new Timestamp(requestedAt.plusSeconds(120)));

            repository.save(payment);
            return paymentId;
        }
    }
}
