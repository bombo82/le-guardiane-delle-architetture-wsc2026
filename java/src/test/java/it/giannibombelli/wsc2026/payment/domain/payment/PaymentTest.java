package it.giannibombelli.wsc2026.payment.domain.payment;

import it.giannibombelli.wsc2026.common.domain.identity.EntityId;
import it.giannibombelli.wsc2026.common.domain.primitive.ClientReference;
import it.giannibombelli.wsc2026.common.domain.primitive.Description;
import it.giannibombelli.wsc2026.common.domain.primitive.Money;
import it.giannibombelli.wsc2026.common.domain.primitive.Timestamp;
import it.giannibombelli.wsc2026.payment.domain.events.PaymentResultEvents;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentTest {

    @Nested
    class Request {
        @Test
        void shouldCreatePaymentInRequestedStatus() {
            PaymentId paymentId = EntityId.generate(PaymentId::new);
            String clientReference = UUID.randomUUID().toString();
            Money amount = new Money(new BigDecimal("50.00"));

            Instant requestedAt = Instant.parse("2026-06-07T10:00:00Z");
            Payment payment = Payment.request(paymentId, new ClientReference(clientReference), amount, new Timestamp(requestedAt));

            assertThat(payment.id()).isEqualTo(paymentId);
            assertThat(payment.clientReference().value()).isEqualTo(clientReference);
            assertThat(payment.amount()).isEqualTo(amount);
            assertThat(payment.status()).isEqualTo(PaymentStatus.REQUESTED);
        }

        @Test
        void shouldFailIfParametersAreNull() {
            PaymentId paymentId = EntityId.generate(PaymentId::new);
            String clientReference = UUID.randomUUID().toString();
            Money amount = new Money(new BigDecimal("50.00"));

            Instant requestedAt = Instant.parse("2026-06-07T10:00:00Z");
            assertThatThrownBy(() -> Payment.request(null, new ClientReference(clientReference), amount, new Timestamp(requestedAt)))
                .isInstanceOf(IllegalArgumentException.class);

            assertThatThrownBy(() -> Payment.request(paymentId, null, amount, new Timestamp(requestedAt)))
                .isInstanceOf(IllegalArgumentException.class);

            assertThatThrownBy(() -> Payment.request(paymentId, new ClientReference(clientReference), null, new Timestamp(requestedAt)))
                .isInstanceOf(IllegalArgumentException.class);

            assertThatThrownBy(() -> Payment.request(paymentId, new ClientReference(clientReference), amount, null))
                .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    class StartTransaction {
        @Test
        void shouldFailIfParametersAreNull() {
            Instant requestedAt = Instant.parse("2026-06-07T10:00:00Z");
            Payment payment = Payment.request(
                EntityId.generate(PaymentId::new),
                new ClientReference(UUID.randomUUID().toString()),
                new Money(new BigDecimal("50.00")),
                new Timestamp(requestedAt)
            );
            TransactionId transactionId = EntityId.generate(TransactionId::new);
            Provider provider = Provider.GIFT_CARD;
            Money amount = new Money(new BigDecimal("50.00"));
            Timestamp startedAt = new Timestamp(requestedAt.plusSeconds(1));

            assertThatThrownBy(() -> payment.startTransaction(null, provider, null, amount, startedAt))
                .isInstanceOf(IllegalArgumentException.class);

            assertThatThrownBy(() -> payment.startTransaction(transactionId, null, null, amount, startedAt))
                .isInstanceOf(IllegalArgumentException.class);

            assertThatThrownBy(() -> payment.startTransaction(transactionId, provider, null, null, startedAt))
                .isInstanceOf(IllegalArgumentException.class);

            assertThatThrownBy(() -> payment.startTransaction(transactionId, provider, null, amount, null))
                .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    class Accept {
        @Test
        void shouldProducePaymentAcceptedEvent() {
            Instant requestedAt = Instant.parse("2026-06-07T10:00:00Z");
            Payment payment = Payment.request(
                EntityId.generate(PaymentId::new),
                new ClientReference(UUID.randomUUID().toString()),
                new Money(new BigDecimal("50.00")),
                new Timestamp(requestedAt)
            );
            TransactionId transactionId = EntityId.generate(TransactionId::new);
            payment.startTransaction(
                transactionId,
                Provider.GIFT_CARD,
                new ProviderReference(UUID.randomUUID()),
                new Money(new BigDecimal("50.00")),
                new Timestamp(requestedAt.plusSeconds(1))
            );

            PaymentResultEvents.PaymentAccepted event = payment.acceptTransaction(
                transactionId,
                new Timestamp(requestedAt.plusSeconds(60))
            );

            assertThat(event.aggregateId()).isEqualTo(payment.id());
            assertThat(event.amount()).isEqualTo(new Money(new BigDecimal("50.00")));
        }

        @Test
        void shouldFailIfParametersAreNull() {
            Instant requestedAt = Instant.parse("2026-06-07T10:00:00Z");
            Payment payment = Payment.request(
                EntityId.generate(PaymentId::new),
                new ClientReference(UUID.randomUUID().toString()),
                new Money(new BigDecimal("50.00")),
                new Timestamp(requestedAt)
            );

            assertThatThrownBy(() -> payment.acceptTransaction(null, null))
                .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    class Reject {
        @Test
        void shouldFailIfParametersAreNull() {
            Instant requestedAt = Instant.parse("2026-06-07T10:00:00Z");
            Payment payment = Payment.request(
                EntityId.generate(PaymentId::new),
                new ClientReference(UUID.randomUUID().toString()),
                new Money(new BigDecimal("50.00")),
                new Timestamp(requestedAt)
            );
            TransactionId transactionId = EntityId.generate(TransactionId::new);
            payment.startTransaction(
                transactionId,
                Provider.GIFT_CARD,
                new ProviderReference(UUID.randomUUID()),
                new Money(new BigDecimal("50.00")),
                new Timestamp(requestedAt.plusSeconds(1))
            );

            assertThatThrownBy(() -> payment.rejectTransaction(null, new Description("reason")))
                .isInstanceOf(IllegalArgumentException.class);

            assertThatThrownBy(() -> payment.rejectTransaction(transactionId, null))
                .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    class RequestRefund {
        @Test
        void shouldFailIfAmountIsNull() {
            Instant requestedAt = Instant.parse("2026-06-07T10:00:00Z");
            Payment payment = Payment.request(
                EntityId.generate(PaymentId::new),
                new ClientReference(UUID.randomUUID().toString()),
                new Money(new BigDecimal("50.00")),
                new Timestamp(requestedAt)
            );
            TransactionId transactionId = EntityId.generate(TransactionId::new);
            payment.startTransaction(
                transactionId,
                Provider.GIFT_CARD,
                new ProviderReference(UUID.randomUUID()),
                new Money(new BigDecimal("50.00")),
                new Timestamp(requestedAt.plusSeconds(1))
            );
            payment.acceptTransaction(transactionId, new Timestamp(requestedAt.plusSeconds(60)));

            assertThatThrownBy(() -> payment.requestRefund(null))
                .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    class RejectRefund {
        @Test
        void shouldFailIfParametersAreNull() {
            Instant requestedAt = Instant.parse("2026-06-07T10:00:00Z");
            Payment payment = Payment.request(
                EntityId.generate(PaymentId::new),
                new ClientReference(UUID.randomUUID().toString()),
                new Money(new BigDecimal("50.00")),
                new Timestamp(requestedAt)
            );
            TransactionId transactionId = EntityId.generate(TransactionId::new);
            payment.startTransaction(
                transactionId,
                Provider.GIFT_CARD,
                new ProviderReference(UUID.randomUUID()),
                new Money(new BigDecimal("50.00")),
                new Timestamp(requestedAt.plusSeconds(1))
            );
            payment.acceptTransaction(transactionId, new Timestamp(requestedAt.plusSeconds(60)));

            Provider provider = Provider.GIFT_CARD;
            ProviderReference providerReference = new ProviderReference(UUID.randomUUID());

            assertThatThrownBy(() -> payment.rejectRefund(null, providerReference, new Description("reason")))
                .isInstanceOf(IllegalArgumentException.class);

            assertThatThrownBy(() -> payment.rejectRefund(provider, null, new Description("reason")))
                .isInstanceOf(IllegalArgumentException.class);

            assertThatThrownBy(() -> payment.rejectRefund(provider, providerReference, null))
                .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
