package it.giannibombelli.wsc2026.payment.application.usecases;

import it.giannibombelli.wsc2026.common.domain.identity.EntityId;
import it.giannibombelli.wsc2026.common.domain.primitive.ClientReference;
import it.giannibombelli.wsc2026.common.domain.primitive.Money;
import it.giannibombelli.wsc2026.common.domain.primitive.Timestamp;
import it.giannibombelli.wsc2026.payment.application.commands.RequestPayment;
import it.giannibombelli.wsc2026.payment.domain.events.PaymentEvent;
import it.giannibombelli.wsc2026.payment.domain.events.PaymentRequested;
import it.giannibombelli.wsc2026.payment.domain.payment.Payment;
import it.giannibombelli.wsc2026.payment.domain.payment.PaymentId;
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

class PaymentRequestingTest {
    private PaymentRepository repository;
    private CapturingEventPublisher<PaymentEvent> publisher;
    private PaymentRequesting paymentRequesting;

    @BeforeEach
    void setUp() {
        final DataSource dataSource = DatabaseSetup.initializeInMemoryDb("payment");
        repository = new SqlitePaymentRepository(dataSource);
        publisher = new CapturingEventPublisher<>();
        paymentRequesting = new PaymentRequesting(repository, publisher);
    }

    @Nested
    class Construction {
        @Test
        void rejectNullRepository() {
            assertThatThrownBy(() -> new PaymentRequesting(null, publisher))
                .isInstanceOf(NullPointerException.class);
        }

        @Test
        void rejectNullPublisher() {
            assertThatThrownBy(() -> new PaymentRequesting(repository, null))
                .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    class Execution {
        @Test
        void rejectNullCommand() {
            assertThatThrownBy(() -> paymentRequesting.invoke(null))
                .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void shouldPersist() {
            PaymentId paymentId = EntityId.generate(PaymentId::new);
            String clientReference = UUID.randomUUID().toString();
            Money amount = new Money(new BigDecimal("75.00"));
            Timestamp requestedAt = new Timestamp(Instant.parse("2026-06-07T10:00:00Z"));

            PaymentRequested event = paymentRequesting.invoke(new RequestPayment(paymentId, new ClientReference(clientReference), amount, requestedAt));

            Optional<Payment> loaded = repository.findById(paymentId);
            assertThat(loaded).isPresent();
            assertThat(loaded.get().id()).isEqualTo(paymentId);
            assertThat(loaded.get().clientReference().value()).isEqualTo(clientReference);
            assertThat(loaded.get().amount()).isEqualTo(amount);
            assertThat(loaded.get().status().name()).isEqualTo("REQUESTED");

            assertThat(event.aggregateId()).isEqualTo(paymentId);
        }

        @Test
        void shouldPublishRequestedEvent() {
            PaymentId paymentId = EntityId.generate(PaymentId::new);
            String clientReference = UUID.randomUUID().toString();
            Money amount = new Money(new BigDecimal("75.00"));
            Timestamp requestedAt = new Timestamp(Instant.parse("2026-06-07T10:00:00Z"));

            paymentRequesting.invoke(new RequestPayment(paymentId, new ClientReference(clientReference), amount, requestedAt));

            assertThat(publisher.events()).hasSize(1);
            assertThat(publisher.events().get(0)).isInstanceOf(PaymentRequested.class);
        }
    }
}
