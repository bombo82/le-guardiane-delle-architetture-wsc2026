package it.giannibombelli.wsc2026.payment.domain.payment;

import it.giannibombelli.wsc2026.common.domain.identity.EntityId;
import it.giannibombelli.wsc2026.common.domain.primitive.ClientReference;
import it.giannibombelli.wsc2026.common.domain.primitive.Description;
import it.giannibombelli.wsc2026.common.domain.primitive.Money;
import it.giannibombelli.wsc2026.common.domain.primitive.Timestamp;
import it.giannibombelli.wsc2026.payment.domain.events.PaymentResultEvents;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Una transazione rifiutata rende il pagamento REJECTED; il pagamento diventa ACCEPTED solo se
 * tutte le transazioni accettate rientrano nella finestra di 48h dalla richiesta.
 */
class PaymentTransactionDecisionTest {

    private static final String CLIENT_REFERENCE = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa";
    private static final Money TOTAL = new Money(new java.math.BigDecimal("100.00"));

    private static final String PAYPAL = "PayPal";
    private static final String KLARNA = "Klarna";

    @Test
    void singleTimelyAcceptLeadsToPaymentAccepted() {
        Instant requestedAt = Instant.parse("2026-06-07T10:00:00Z");
        Instant providerCompletion = Instant.parse("2026-06-07T11:00:00Z");

        PaymentId pid = new PaymentId(UUID.randomUUID());
        Payment p = Payment.request(pid, new ClientReference(CLIENT_REFERENCE), TOTAL, new Timestamp(requestedAt));
        TransactionId txId = startTransaction(p, PAYPAL, TOTAL, requestedAt.plusSeconds(1));

        PaymentResultEvents evt = p.acceptTransaction(txId, new Timestamp(providerCompletion));

        assertThat(evt).isInstanceOf(PaymentResultEvents.PaymentAccepted.class);
        assertThat(p.status()).isEqualTo(PaymentStatus.ACCEPTED);
    }

    @Test
    void anySingleRejectRejectsTheEntirePayment() {
        Instant requestedAt = Instant.parse("2026-06-07T09:00:00Z");

        PaymentId pid = new PaymentId(UUID.randomUUID());
        Payment p = Payment.request(pid, new ClientReference(CLIENT_REFERENCE), TOTAL, new Timestamp(requestedAt));
        TransactionId txA = startTransaction(p, PAYPAL, new Money(new java.math.BigDecimal("40.00")), requestedAt.plusSeconds(1));
        p.acceptTransaction(txA, new Timestamp(requestedAt.plusSeconds(3600)));

        TransactionId txB = startTransaction(p, KLARNA, new Money(new java.math.BigDecimal("60.00")), requestedAt.plusSeconds(2));
        PaymentResultEvents evt = p.rejectTransaction(txB, new Description("declined"));

        assertThat(evt).isInstanceOf(PaymentResultEvents.PaymentRejected.class);
        assertThat(p.status()).isEqualTo(PaymentStatus.REJECTED);
    }

    @Test
    void allAcceptedButInsufficientCoverageStaysProcessingNotAccepted() {
        Instant requestedAt = Instant.parse("2026-06-07T10:00:00Z");
        Instant txTime = Instant.parse("2026-06-07T10:30:00Z");

        PaymentId pid = new PaymentId(UUID.randomUUID());
        Payment p = Payment.request(pid, new ClientReference(CLIENT_REFERENCE), TOTAL, new Timestamp(requestedAt));
        TransactionId txId = startTransaction(p, PAYPAL, new Money(new java.math.BigDecimal("60.00")), requestedAt.plusSeconds(1));

        p.acceptTransaction(txId, new Timestamp(txTime));

        assertThat(p.status()).isEqualTo(PaymentStatus.PROCESSING);
    }

    @Test
    void paymentAcceptedOnlyWhenAllTxAcceptedAndAllTimesWithin48hWindow() {
        Instant requestedAt = Instant.parse("2026-06-07T08:00:00Z");
        Instant tx1 = Instant.parse("2026-06-07T09:00:00Z");
        Instant tx2 = Instant.parse("2026-06-08T06:59:59Z");

        PaymentId pid = new PaymentId(UUID.randomUUID());
        Payment p = Payment.request(pid, new ClientReference(CLIENT_REFERENCE), TOTAL, new Timestamp(requestedAt));
        TransactionId txA = startTransaction(p, PAYPAL, new Money(new java.math.BigDecimal("60.00")), requestedAt.plusSeconds(1));
        p.acceptTransaction(txA, new Timestamp(tx1));
        TransactionId txB = startTransaction(p, KLARNA, new Money(new java.math.BigDecimal("40.00")), requestedAt.plusSeconds(2));
        PaymentResultEvents finalEvt = p.acceptTransaction(txB, new Timestamp(tx2));

        assertThat(finalEvt).isInstanceOf(PaymentResultEvents.PaymentAccepted.class);
        assertThat(p.status()).isEqualTo(PaymentStatus.ACCEPTED);
    }

    @Test
    void providerTimeBeyond48hDoesNotCountAsTimelyAcceptForFinalState() {
        Instant requestedAt = Instant.parse("2026-06-01T12:00:00Z");
        Instant timely = Instant.parse("2026-06-01T13:00:00Z");
        Instant late = Instant.parse("2026-06-06T00:00:00Z");

        PaymentId pid = new PaymentId(UUID.randomUUID());
        Payment p = Payment.request(pid, new ClientReference(CLIENT_REFERENCE), TOTAL, new Timestamp(requestedAt));
        TransactionId txA = startTransaction(p, PAYPAL, new Money(new java.math.BigDecimal("50.00")), requestedAt.plusSeconds(1));
        p.acceptTransaction(txA, new Timestamp(timely));
        TransactionId txB = startTransaction(p, KLARNA, new Money(new java.math.BigDecimal("50.00")), requestedAt.plusSeconds(2));

        assertThatThrownBy(() ->
            p.acceptTransaction(txB, new Timestamp(late))
        ).isInstanceOf(IllegalStateException.class);

        assertThat(p.status()).isEqualTo(PaymentStatus.PROCESSING);
    }

    private TransactionId startTransaction(Payment payment, String provider, Money amount, Instant startedAt) {
        TransactionId transactionId = EntityId.generate(TransactionId::new);
        payment.startTransaction(
            transactionId,
            Provider.fromLabel(provider),
            new ProviderReference(UUID.randomUUID()),
            amount,
            new Timestamp(startedAt)
        );
        return transactionId;
    }
}
