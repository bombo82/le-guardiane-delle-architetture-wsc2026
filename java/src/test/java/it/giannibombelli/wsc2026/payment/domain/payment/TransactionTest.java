package it.giannibombelli.wsc2026.payment.domain.payment;

import it.giannibombelli.wsc2026.common.domain.identity.EntityId;
import it.giannibombelli.wsc2026.common.domain.primitive.Money;
import it.giannibombelli.wsc2026.common.domain.primitive.Timestamp;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TransactionTest {

    @Nested
    class Start {
        @Test
        void shouldCreateStartedTransaction() {
            TransactionId transactionId = EntityId.generate(TransactionId::new);
            Provider provider = Provider.GIFT_CARD;
            ProviderReference providerReference = new ProviderReference(UUID.randomUUID());
            Money amount = new Money(BigDecimal.TEN);
            Timestamp startedAt = new Timestamp(Instant.parse("2026-06-07T10:00:00Z"));

            Transaction transaction = Transaction.start(transactionId, provider, providerReference, amount, startedAt);

            assertThat(transaction.id()).isEqualTo(transactionId);
            assertThat(transaction.provider()).isEqualTo(provider);
            assertThat(transaction.providerReference()).isEqualTo(providerReference);
            assertThat(transaction.amount()).isEqualTo(amount);
            assertThat(transaction.status()).isEqualTo(TransactionStatus.STARTED);
            assertThat(transaction.startedAt()).isEqualTo(startedAt);
            assertThat(transaction.completedAt()).isNull();
        }

        @Test
        void shouldFailIfParametersAreNull() {
            TransactionId transactionId = EntityId.generate(TransactionId::new);
            Provider provider = Provider.GIFT_CARD;
            ProviderReference providerReference = new ProviderReference(UUID.randomUUID());
            Money amount = new Money(BigDecimal.TEN);
            Timestamp startedAt = new Timestamp(Instant.parse("2026-06-07T10:00:00Z"));

            assertThatThrownBy(() -> Transaction.start(null, provider, providerReference, amount, startedAt))
                .isInstanceOf(IllegalArgumentException.class);

            assertThatThrownBy(() -> Transaction.start(transactionId, null, providerReference, amount, startedAt))
                .isInstanceOf(IllegalArgumentException.class);

            assertThatThrownBy(() -> Transaction.start(transactionId, provider, providerReference, null, startedAt))
                .isInstanceOf(IllegalArgumentException.class);

            assertThatThrownBy(() -> Transaction.start(transactionId, provider, providerReference, amount, null))
                .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    class Accept {
        @Test
        void shouldFailIfCompletedAtIsNull() {
            Transaction transaction = createStartedTransaction();

            assertThatThrownBy(() -> transaction.accept((Timestamp) null))
                .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void shouldFailIfTransactionIsNotStarted() {
            Transaction accepted = createStartedTransaction().accept(Timestamp.now());

            assertThatThrownBy(() -> accepted.accept(Timestamp.now()))
                .isInstanceOf(IllegalStateException.class);
        }
    }

    @Nested
    class Reject {
        @Test
        void shouldFailIfCompletedAtIsNull() {
            Transaction transaction = createStartedTransaction();

            assertThatThrownBy(() -> transaction.reject((Timestamp) null))
                .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void shouldFailIfTransactionIsNotStarted() {
            Transaction rejected = createStartedTransaction().reject(Timestamp.now());

            assertThatThrownBy(() -> rejected.reject(Timestamp.now()))
                .isInstanceOf(IllegalStateException.class);
        }
    }

    private Transaction createStartedTransaction() {
        return Transaction.start(
            EntityId.generate(TransactionId::new),
            Provider.GIFT_CARD,
            new ProviderReference(UUID.randomUUID()),
            new Money(BigDecimal.TEN),
            new Timestamp(Instant.parse("2026-06-07T10:00:00Z"))
        );
    }
}
