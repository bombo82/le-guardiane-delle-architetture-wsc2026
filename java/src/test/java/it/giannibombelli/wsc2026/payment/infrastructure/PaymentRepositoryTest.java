package it.giannibombelli.wsc2026.payment.infrastructure;

import it.giannibombelli.wsc2026.common.domain.identity.EntityId;
import it.giannibombelli.wsc2026.common.domain.primitive.ClientReference;
import it.giannibombelli.wsc2026.common.domain.primitive.Money;
import it.giannibombelli.wsc2026.common.domain.primitive.Timestamp;
import it.giannibombelli.wsc2026.payment.domain.payment.Payment;
import it.giannibombelli.wsc2026.payment.domain.payment.PaymentId;
import it.giannibombelli.wsc2026.payment.domain.payment.PaymentStatus;
import it.giannibombelli.wsc2026.payment.domain.ports.PaymentRepository;
import it.giannibombelli.wsc2026.testsupport.DatabaseSetup;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifica il contratto del repository senza ripetere le regole di business di dominio.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PaymentRepositoryTest {
    private PaymentRepository repository;

    @BeforeAll
    void setUp() {
        final DataSource dataSource = DatabaseSetup.initializeFileDb("payment", getClass().getSimpleName());
        repository = new SqlitePaymentRepository(dataSource);
    }

    @Nested
    class Save {
        @Test
        void shouldPersistNewPayment() {
            PaymentId paymentId = EntityId.generate(PaymentId::new);
            String clientReference = UUID.randomUUID().toString();
            Money amount = new Money(new BigDecimal("50.00"));
            Instant requestedAt = Instant.parse("2026-06-07T10:00:00Z");
            Payment original = Payment.request(paymentId, new ClientReference(clientReference), amount, new Timestamp(requestedAt));

            repository.save(original);
            Optional<Payment> reloaded = repository.findById(paymentId);

            assertThat(reloaded).isPresent();
            Payment found = reloaded.get();
            assertThat(found.id()).isEqualTo(paymentId);
            assertThat(found.clientReference().value()).isEqualTo(clientReference);
            assertThat(found.amount()).isEqualTo(amount);
            assertThat(found.status()).isEqualTo(PaymentStatus.REQUESTED);
            assertThat(found.requestedAt().value()).isEqualTo(requestedAt);
        }
    }

    @Nested
    class FindById {
        @Test
        void shouldReturnEmptyWhenNotFound() {
            PaymentId nonExistentId = EntityId.generate(PaymentId::new);

            Optional<Payment> reloaded = repository.findById(nonExistentId);

            assertThat(reloaded).isEmpty();
        }
    }

    @Nested
    class FindByClientReference {
        @Test
        void shouldReturnPaymentWhenClientReferenceMatches() {
            PaymentId paymentId = EntityId.generate(PaymentId::new);
            String clientReference = UUID.randomUUID().toString();
            Money amount = new Money(new BigDecimal("25.00"));
            Payment payment = Payment.request(paymentId, new ClientReference(clientReference), amount, new Timestamp(Instant.now()));
            repository.save(payment);

            Optional<Payment> found = repository.findByClientReference(new ClientReference(clientReference));

            assertThat(found).isPresent();
            assertThat(found.get().id()).isEqualTo(paymentId);
        }

        @Test
        void shouldReturnEmptyWhenClientReferenceDoesNotMatch() {
            Optional<Payment> found = repository.findByClientReference(new ClientReference("non-existing-reference"));

            assertThat(found).isEmpty();
        }
    }

    @Nested
    class FindAllRequestedAndProcessingBefore {
        @Test
        void shouldReturnPaymentsRequestedBeforeThreshold() {
            PaymentId paymentId = EntityId.generate(PaymentId::new);
            Instant requestedAt = Instant.parse("2026-06-07T10:00:00Z");
            Payment payment = Payment.request(paymentId, new ClientReference(UUID.randomUUID().toString()), new Money(new BigDecimal("10.00")), new Timestamp(requestedAt));
            repository.save(payment);

            List<Payment> found = repository.findAllRequestedAndProcessingBefore(new Timestamp(Instant.parse("2026-06-08T10:00:00Z")));

            assertThat(found).extracting(Payment::id).contains(paymentId);
        }

        @Test
        void shouldNotReturnPaymentsRequestedAfterThreshold() {
            PaymentId paymentId = EntityId.generate(PaymentId::new);
            Instant requestedAt = Instant.parse("2026-06-07T10:00:00Z");
            Payment payment = Payment.request(paymentId, new ClientReference(UUID.randomUUID().toString()), new Money(new BigDecimal("10.00")), new Timestamp(requestedAt));
            repository.save(payment);

            List<Payment> found = repository.findAllRequestedAndProcessingBefore(new Timestamp(Instant.parse("2026-06-06T10:00:00Z")));

            assertThat(found).extracting(Payment::id).doesNotContain(paymentId);
        }
    }
}
