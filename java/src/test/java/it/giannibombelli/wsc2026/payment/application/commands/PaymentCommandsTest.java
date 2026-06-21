package it.giannibombelli.wsc2026.payment.application.commands;

import it.giannibombelli.wsc2026.common.domain.identity.EntityId;
import it.giannibombelli.wsc2026.common.domain.primitive.ClientReference;
import it.giannibombelli.wsc2026.common.domain.primitive.Description;
import it.giannibombelli.wsc2026.common.domain.primitive.Money;
import it.giannibombelli.wsc2026.common.domain.primitive.Timestamp;
import it.giannibombelli.wsc2026.payment.domain.payment.PaymentId;
import it.giannibombelli.wsc2026.payment.domain.payment.Provider;
import it.giannibombelli.wsc2026.payment.domain.payment.ProviderReference;
import it.giannibombelli.wsc2026.payment.domain.payment.TransactionId;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentCommandsTest {

    @Nested
    class RequestPaymentValidation {
        @Test
        void shouldFailIfParametersAreNullOrBlank() {
            PaymentId paymentId = EntityId.generate(PaymentId::new);
            Money amount = new Money(BigDecimal.TEN);
            Timestamp requestedAt = new Timestamp(Instant.parse("2026-06-07T10:00:00Z"));

            assertThatThrownBy(() -> new RequestPayment(null, new ClientReference(UUID.fromString("00000000-0000-0000-0000-000000000001")), amount, requestedAt))
                .isInstanceOf(IllegalArgumentException.class);

            assertThatThrownBy(() -> new RequestPayment(paymentId, null, amount, requestedAt))
                .isInstanceOf(IllegalArgumentException.class);

            assertThatThrownBy(() -> new RequestPayment(paymentId, new ClientReference((UUID) null), amount, requestedAt))
                .isInstanceOf(IllegalArgumentException.class);

            assertThatThrownBy(() -> new RequestPayment(paymentId, new ClientReference(UUID.fromString("00000000-0000-0000-0000-000000000001")), null, requestedAt))
                .isInstanceOf(IllegalArgumentException.class);

            assertThatThrownBy(() -> new RequestPayment(paymentId, new ClientReference(UUID.fromString("00000000-0000-0000-0000-000000000001")), amount, null))
                .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    class StartTransactionValidation {
        @Test
        void shouldFailIfParametersAreNull() {
            PaymentId paymentId = EntityId.generate(PaymentId::new);
            UUID providerReference = UUID.randomUUID();
            Money amount = new Money(BigDecimal.TEN);
            Timestamp startedAt = new Timestamp(Instant.parse("2026-06-07T10:00:00Z"));
            ProviderReference providerReferenceVo = new ProviderReference(providerReference);

            assertThatThrownBy(() -> new StartTransaction(null, Provider.GIFT_CARD, providerReferenceVo, amount, startedAt))
                .isInstanceOf(IllegalArgumentException.class);

            assertThatThrownBy(() -> new StartTransaction(paymentId, null, providerReferenceVo, amount, startedAt))
                .isInstanceOf(IllegalArgumentException.class);

            assertThatThrownBy(() -> new StartTransaction(paymentId, Provider.GIFT_CARD, providerReferenceVo, null, startedAt))
                .isInstanceOf(IllegalArgumentException.class);

            assertThatThrownBy(() -> new StartTransaction(paymentId, Provider.GIFT_CARD, providerReferenceVo, amount, null))
                .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    class RefundTransactionValidation {
        @Test
        void shouldFailIfParametersAreNull() {
            PaymentId paymentId = EntityId.generate(PaymentId::new);
            Money amount = new Money(BigDecimal.TEN);

            assertThatThrownBy(() -> new RefundTransaction(null, amount))
                .isInstanceOf(IllegalArgumentException.class);

            assertThatThrownBy(() -> new RefundTransaction(paymentId, null))
                .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    class RejectTransactionValidation {
        @Test
        void shouldFailIfParametersAreNullOrBlank() {
            PaymentId paymentId = EntityId.generate(PaymentId::new);
            TransactionId transactionId = EntityId.generate(TransactionId::new);

            assertThatThrownBy(() -> new RejectTransaction(null, transactionId, new Description("reason")))
                .isInstanceOf(IllegalArgumentException.class);

            assertThatThrownBy(() -> new RejectTransaction(paymentId, null, new Description("reason")))
                .isInstanceOf(IllegalArgumentException.class);

            assertThatThrownBy(() -> new RejectTransaction(paymentId, transactionId, null))
                .isInstanceOf(IllegalArgumentException.class);

            assertThatThrownBy(() -> new RejectTransaction(paymentId, transactionId, new Description("")))
                .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    class AcceptTransactionValidation {
        @Test
        void shouldFailIfParametersAreNull() {
            PaymentId paymentId = EntityId.generate(PaymentId::new);
            TransactionId transactionId = EntityId.generate(TransactionId::new);
            Timestamp providerCompletedAt = new Timestamp(Instant.parse("2026-06-07T10:00:00Z"));

            assertThatThrownBy(() -> new AcceptTransaction(null, transactionId, providerCompletedAt))
                .isInstanceOf(IllegalArgumentException.class);

            assertThatThrownBy(() -> new AcceptTransaction(paymentId, null, providerCompletedAt))
                .isInstanceOf(IllegalArgumentException.class);

            assertThatThrownBy(() -> new AcceptTransaction(paymentId, transactionId, null))
                .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    class ExpirePaymentValidation {
        @Test
        void shouldFailIfAggregateIdIsNull() {
            assertThatThrownBy(() -> new ExpirePayment(null))
                .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
