package it.giannibombelli.wsc2026.payment.application.usecases;

import it.giannibombelli.wsc2026.common.domain.identity.EntityId;
import it.giannibombelli.wsc2026.common.domain.primitive.ClientReference;
import it.giannibombelli.wsc2026.common.domain.primitive.Money;
import it.giannibombelli.wsc2026.common.domain.primitive.Timestamp;
import it.giannibombelli.wsc2026.payment.domain.events.PaymentDeadlineReached;
import it.giannibombelli.wsc2026.payment.domain.events.PaymentEvent;
import it.giannibombelli.wsc2026.payment.domain.events.PaymentResultEvents;
import it.giannibombelli.wsc2026.payment.domain.payment.Payment;
import it.giannibombelli.wsc2026.payment.domain.payment.PaymentId;
import it.giannibombelli.wsc2026.payment.domain.payment.PaymentStatus;
import it.giannibombelli.wsc2026.payment.application.policies.PaymentExpiration;
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

class PaymentExpiringTest {
    private PaymentRepository repository;
    private CapturingEventPublisher<PaymentEvent> publisher;
    private PaymentExpiring paymentExpiring;

    @BeforeEach
    void setUp() {
        final DataSource dataSource = DatabaseSetup.initializeInMemoryDb("payment");
        repository = new SqlitePaymentRepository(dataSource);
        publisher = new CapturingEventPublisher<>();
        paymentExpiring = new PaymentExpiring(repository, publisher, new PaymentExpiration());
    }

    @Nested
    class Construction {
        @Test
        void rejectNullRepository() {
            assertThatThrownBy(() -> new PaymentExpiring(null, publisher, new PaymentExpiration()))
                .isInstanceOf(NullPointerException.class);
        }

        @Test
        void rejectNullPublisher() {
            assertThatThrownBy(() -> new PaymentExpiring(repository, null, new PaymentExpiration()))
                .isInstanceOf(NullPointerException.class);
        }

        @Test
        void rejectNullPolicy() {
            assertThatThrownBy(() -> new PaymentExpiring(repository, publisher, null))
                .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    class Execution {
        @Test
        void rejectNullEvent() {
            assertThatThrownBy(() -> paymentExpiring.on(null))
                .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void shouldExpirePayment() {
            PaymentId paymentId = EntityId.generate(PaymentId::new);
            UUID clientReference = UUID.randomUUID();
            Money amount = new Money(new BigDecimal("50.00"));
            Instant requestedAt = Instant.parse("2026-06-07T10:00:00Z");
            Payment payment = Payment.request(paymentId, new ClientReference(clientReference), amount, new Timestamp(requestedAt));
            repository.save(payment);

            paymentExpiring.on(new PaymentDeadlineReached(paymentId));

            Optional<Payment> loaded = repository.findById(paymentId);
            assertThat(loaded).isPresent();
            assertThat(loaded.get().status()).isEqualTo(PaymentStatus.EXPIRED);
        }

        @Test
        void shouldPublishExpiredEvent() {
            PaymentId paymentId = EntityId.generate(PaymentId::new);
            UUID clientReference = UUID.randomUUID();
            Money amount = new Money(new BigDecimal("50.00"));
            Instant requestedAt = Instant.parse("2026-06-07T10:00:00Z");
            Payment payment = Payment.request(paymentId, new ClientReference(clientReference), amount, new Timestamp(requestedAt));
            repository.save(payment);

            paymentExpiring.on(new PaymentDeadlineReached(paymentId));

            assertThat(publisher.events()).hasSize(1);
            assertThat(publisher.events().get(0)).isInstanceOf(PaymentResultEvents.PaymentExpired.class);
            PaymentResultEvents.PaymentExpired expired = (PaymentResultEvents.PaymentExpired) publisher.events().get(0);
            assertThat(expired.aggregateId()).isEqualTo(paymentId);
            assertThat(expired.clientReference().value()).isEqualTo(clientReference);
        }

        @Test
        void shouldFailIfPaymentNotFound() {
            PaymentId paymentId = EntityId.generate(PaymentId::new);

            assertThatThrownBy(() -> paymentExpiring.on(new PaymentDeadlineReached(paymentId)))
                .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
