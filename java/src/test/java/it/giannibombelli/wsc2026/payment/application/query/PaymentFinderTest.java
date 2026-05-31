package it.giannibombelli.wsc2026.payment.application.query;

import it.giannibombelli.wsc2026.common.domain.identity.EntityId;
import it.giannibombelli.wsc2026.common.domain.primitive.ClientReference;
import it.giannibombelli.wsc2026.common.domain.primitive.Money;
import it.giannibombelli.wsc2026.common.domain.primitive.Timestamp;
import it.giannibombelli.wsc2026.payment.domain.payment.Payment;
import it.giannibombelli.wsc2026.payment.domain.payment.PaymentId;
import it.giannibombelli.wsc2026.payment.domain.payment.PaymentStatus;
import it.giannibombelli.wsc2026.payment.infrastructure.SqlitePaymentRepository;
import it.giannibombelli.wsc2026.testsupport.DatabaseSetup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentFinderTest {
    private SqlitePaymentRepository repository;
    private PaymentFinder finder;

    @BeforeEach
    void setUp() {
        final DataSource dataSource = DatabaseSetup.initializeInMemoryDb("payment");
        repository = new SqlitePaymentRepository(dataSource);
        finder = new PaymentFinder(repository);
    }

    @Test
    void shouldFindPaymentSummaryById() {
        PaymentId paymentId = EntityId.generate(PaymentId::new);
        String clientReference = UUID.randomUUID().toString();
        Money amount = new Money(new BigDecimal("50.00"));
        Instant requestedAt = Instant.parse("2026-06-07T10:00:00Z");
        Payment payment = Payment.request(paymentId, new ClientReference(clientReference), amount, new Timestamp(requestedAt));
        repository.save(payment);

        Optional<PaymentSummary> result = finder.findSummaryById(paymentId);

        assertThat(result).isPresent();
        assertThat(result.get().id()).isEqualTo(paymentId.value());
        assertThat(result.get().clientReference().value()).isEqualTo(clientReference);
        assertThat(result.get().amount()).isEqualTo(amount);
        assertThat(result.get().status()).isEqualTo(PaymentStatus.REQUESTED);
    }

    @Test
    void shouldFindPaymentDetailsById() {
        PaymentId paymentId = EntityId.generate(PaymentId::new);
        String clientReference = UUID.randomUUID().toString();
        Money amount = new Money(new BigDecimal("50.00"));
        Instant requestedAt = Instant.parse("2026-06-07T10:00:00Z");
        Payment payment = Payment.request(paymentId, new ClientReference(clientReference), amount, new Timestamp(requestedAt));
        repository.save(payment);

        Optional<PaymentDetails> result = finder.findDetailsById(paymentId);

        assertThat(result).isPresent();
        assertThat(result.get().id()).isEqualTo(paymentId.value());
        assertThat(result.get().transactions()).isEmpty();
    }

    @Test
    void shouldReturnEmptyWhenPaymentNotFound() {
        PaymentId paymentId = EntityId.generate(PaymentId::new);

        Optional<PaymentSummary> result = finder.findSummaryById(paymentId);

        assertThat(result).isEmpty();
    }

    @Test
    void shouldThrowWhenIdIsNull() {
        assertThatThrownBy(() -> finder.findSummaryById(null))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
