package it.giannibombelli.wsc2026.payment.application.usecases;

import it.giannibombelli.wsc2026.common.domain.identity.EntityId;
import it.giannibombelli.wsc2026.common.domain.primitive.ClientReference;
import it.giannibombelli.wsc2026.common.domain.primitive.Money;
import it.giannibombelli.wsc2026.common.domain.primitive.Timestamp;
import it.giannibombelli.wsc2026.payment.application.commands.RefundTransaction;
import it.giannibombelli.wsc2026.payment.domain.events.PaymentEvent;
import it.giannibombelli.wsc2026.payment.domain.events.RefundRequested;
import it.giannibombelli.wsc2026.payment.domain.payment.Payment;
import it.giannibombelli.wsc2026.payment.domain.payment.PaymentId;
import it.giannibombelli.wsc2026.payment.domain.payment.Provider;
import it.giannibombelli.wsc2026.payment.domain.payment.ProviderReference;
import it.giannibombelli.wsc2026.payment.domain.payment.TransactionId;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RefundRequestingTest {
    private PaymentRepository repository;
    private CapturingEventPublisher<PaymentEvent> publisher;
    private RefundRequesting refundRequesting;

    @BeforeEach
    void setUp() {
        final DataSource dataSource = DatabaseSetup.initializeInMemoryDb("payment");
        repository = new SqlitePaymentRepository(dataSource);
        publisher = new CapturingEventPublisher<>();
        refundRequesting = new RefundRequesting(repository, publisher);
    }

    @Nested
    class Construction {
        @Test
        void rejectNullRepository() {
            assertThatThrownBy(() -> new RefundRequesting(null, publisher))
                .isInstanceOf(NullPointerException.class);
        }

        @Test
        void rejectNullPublisher() {
            assertThatThrownBy(() -> new RefundRequesting(repository, null))
                .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    class Execution {
        @Test
        void rejectNullCommand() {
            assertThatThrownBy(() -> refundRequesting.invoke(null))
                .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void shouldPublishRefundRequested() {
            PaymentId paymentId = seedAcceptedPayment(new BigDecimal("50.00"));
            Money refundAmount = new Money(new BigDecimal("50.00"));

            RefundRequested event = refundRequesting.invoke(new RefundTransaction(paymentId, refundAmount));

            assertThat(event.aggregateId()).isEqualTo(paymentId);
            assertThat(event.amount()).isEqualTo(refundAmount);
            assertThat(publisher.events()).hasSize(1);
            assertThat(publisher.events().get(0)).isInstanceOf(RefundRequested.class);
        }

        @Test
        void shouldFailIfPaymentNotFound() {
            PaymentId paymentId = EntityId.generate(PaymentId::new);
            Money refundAmount = new Money(new BigDecimal("10.00"));

            assertThatThrownBy(() -> refundRequesting.invoke(new RefundTransaction(paymentId, refundAmount)))
                .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void shouldFailIfPaymentNotAccepted() {
            PaymentId paymentId = EntityId.generate(PaymentId::new);
            UUID clientReference = UUID.randomUUID();
            Money amount = new Money(new BigDecimal("50.00"));
            Payment payment = Payment.request(paymentId, new ClientReference(clientReference), amount, new Timestamp(Instant.now()));
            repository.save(payment);

            Money refundAmount = new Money(new BigDecimal("10.00"));
            assertThatThrownBy(() -> refundRequesting.invoke(new RefundTransaction(paymentId, refundAmount)))
                .isInstanceOf(IllegalStateException.class);
        }

        private PaymentId seedAcceptedPayment(BigDecimal amount) {
            PaymentId paymentId = EntityId.generate(PaymentId::new);
            UUID clientReference = UUID.randomUUID();
            Money money = new Money(amount);
            Instant requestedAt = Instant.now();
            Payment payment = Payment.request(paymentId, new ClientReference(clientReference), money, new Timestamp(requestedAt));
            TransactionId transactionId = EntityId.generate(TransactionId::new);
            payment.startTransaction(
                transactionId,
                Provider.PAYPAL,
                new ProviderReference(UUID.randomUUID()),
                money,
                new Timestamp(requestedAt.plusSeconds(1))
            );
            payment.acceptTransaction(transactionId, new Timestamp(requestedAt.plusSeconds(60)));
            repository.save(payment);
            return paymentId;
        }
    }
}
