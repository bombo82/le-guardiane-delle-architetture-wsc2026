package it.giannibombelli.wsc2026.payment.application.policies;

import it.giannibombelli.wsc2026.common.domain.identity.EntityId;
import it.giannibombelli.wsc2026.common.domain.primitive.ClientReference;
import it.giannibombelli.wsc2026.common.domain.primitive.Money;
import it.giannibombelli.wsc2026.common.domain.primitive.Timestamp;
import it.giannibombelli.wsc2026.payment.application.commands.ExpirePayment;
import it.giannibombelli.wsc2026.payment.domain.events.PaymentDeadlineReached;
import it.giannibombelli.wsc2026.payment.domain.payment.Payment;
import it.giannibombelli.wsc2026.payment.domain.payment.PaymentId;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentExpirationTest {

    private final PaymentExpiration policy = new PaymentExpiration();

    @Test
    void isDeadlineReached_returnsTrue_whenDeadlinePassed() {
        Instant requestedAt = Instant.parse("2026-06-01T00:00:00Z");
        Instant now = requestedAt.plusSeconds(48 * 3600 + 1);
        Payment payment = paymentRequestedAt(requestedAt);

        assertThat(policy.isDeadlineReached(payment, new Timestamp(now))).isTrue();
    }

    @Test
    void isDeadlineReached_returnsFalse_whenDeadlineNotPassed() {
        Instant requestedAt = Instant.parse("2026-06-01T00:00:00Z");
        Instant now = requestedAt.plusSeconds(47 * 3600);
        Payment payment = paymentRequestedAt(requestedAt);

        assertThat(policy.isDeadlineReached(payment, new Timestamp(now))).isFalse();
    }

    @Test
    void isDeadlineReached_returnsTrue_exactlyAtDeadline() {
        Instant requestedAt = Instant.parse("2026-06-01T00:00:00Z");
        Instant now = requestedAt.plusSeconds(48 * 3600);
        Payment payment = paymentRequestedAt(requestedAt);

        assertThat(policy.isDeadlineReached(payment, new Timestamp(now))).isTrue();
    }

    @Test
    void isDeadlineReached_rejectsNullPayment() {
        assertThatThrownBy(() -> policy.isDeadlineReached(null, Timestamp.now()))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void isDeadlineReached_rejectsNullNow() {
        Payment payment = paymentRequestedAt(Instant.now());

        assertThatThrownBy(() -> policy.isDeadlineReached(payment, null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void evaluate_returnsExpirePaymentCommand() {
        Payment payment = paymentRequestedAt(Instant.now());
        PaymentDeadlineReached event = new PaymentDeadlineReached(payment.id());

        ExpirePayment cmd = policy.evaluate(event);

        assertThat(cmd.aggregateId()).isEqualTo(payment.id());
    }

    @Test
    void evaluate_rejectsNullEvent() {
        assertThatThrownBy(() -> policy.evaluate(null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    private Payment paymentRequestedAt(Instant requestedAt) {
        PaymentId paymentId = EntityId.generate(PaymentId::new);
        return Payment.request(
            paymentId,
            new ClientReference(UUID.randomUUID()),
            new Money(new BigDecimal("50.00")),
            new Timestamp(requestedAt)
        );
    }
}
