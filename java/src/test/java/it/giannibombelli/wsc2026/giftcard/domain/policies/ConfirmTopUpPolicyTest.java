package it.giannibombelli.wsc2026.giftcard.domain.policies;

import it.giannibombelli.wsc2026.common.domain.identity.EntityId;
import it.giannibombelli.wsc2026.common.domain.primitive.Description;
import it.giannibombelli.wsc2026.common.domain.primitive.Money;
import it.giannibombelli.wsc2026.giftcard.application.commands.ConfirmTopUp;
import it.giannibombelli.wsc2026.giftcard.domain.giftcard.GiftCardId;
import it.giannibombelli.wsc2026.payment.domain.events.PaymentResultEvents;
import it.giannibombelli.wsc2026.payment.domain.payment.Payment;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static it.giannibombelli.wsc2026.testsupport.AggregateFactory.createPayment;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConfirmTopUpPolicyTest {

    private final ConfirmTopUpPolicy policy = new ConfirmTopUpPolicy();

    @Test
    void evaluate_onPaymentAccepted_withGiftCardProvider_returnsConfirmTopUp() {
        GiftCardId giftCardId = EntityId.generate(GiftCardId::new);
        Money amount = new Money(new BigDecimal("50.00"));
        Payment payment = createPayment(giftCardId.value().toString(), amount);
        PaymentResultEvents.PaymentAccepted event = new PaymentResultEvents.PaymentAccepted(
            payment.id(), payment.clientReference(), amount
        );

        ConfirmTopUp result = policy.evaluate(event);

        assertThat(result).isNotNull();
        assertThat(result.aggregateId()).isEqualTo(giftCardId);
        assertThat(result.amount()).isEqualTo(amount);
    }

    @Test
    void evaluate_onPaymentAccepted_withNonGiftCardProvider_returnsConfirmTopUp() {
        GiftCardId giftCardId = EntityId.generate(GiftCardId::new);
        Money amount = new Money(new BigDecimal("50.00"));
        Payment payment = createPayment(giftCardId.value().toString(), amount);
        PaymentResultEvents.PaymentAccepted event = new PaymentResultEvents.PaymentAccepted(
            payment.id(), payment.clientReference(), amount
        );

        ConfirmTopUp result = policy.evaluate(event);

        assertThat(result).isNotNull();
        assertThat(result.aggregateId()).isEqualTo(giftCardId);
        assertThat(result.amount()).isEqualTo(amount);
    }

    @Test
    void evaluate_onPaymentRejected_returnsNull() {
        Payment payment = createPayment();
        PaymentResultEvents.PaymentRejected event = new PaymentResultEvents.PaymentRejected(
            payment.id(), payment.clientReference(), payment.amount(), new Description("declined")
        );

        ConfirmTopUp result = policy.evaluate(event);

        assertThat(result).isNull();
    }

    @Test
    void evaluate_onPaymentExpired_returnsNull() {
        Payment payment = createPayment();
        PaymentResultEvents.PaymentExpired event = new PaymentResultEvents.PaymentExpired(
            payment.id(), payment.clientReference(), payment.amount()
        );

        ConfirmTopUp result = policy.evaluate(event);

        assertThat(result).isNull();
    }

    @Test
    void evaluate_onNullEvent_throwsException() {
        assertThatThrownBy(() -> policy.evaluate(null))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
