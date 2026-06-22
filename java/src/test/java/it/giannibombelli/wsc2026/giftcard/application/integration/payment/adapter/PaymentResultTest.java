package it.giannibombelli.wsc2026.giftcard.application.integration.payment.adapter;

import it.giannibombelli.wsc2026.common.domain.identity.EntityId;
import it.giannibombelli.wsc2026.common.domain.primitive.Money;
import it.giannibombelli.wsc2026.giftcard.application.commands.ConfirmTopUp;
import it.giannibombelli.wsc2026.giftcard.domain.giftcard.GiftCardId;
import it.giannibombelli.wsc2026.payment.integration.PaymentResultIntegrationEvent;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentResultTest {

    private final PaymentResult paymentResult = new PaymentResult();

    @Test
    void adaptPaymentAccepted_returnsConfirmTopUp() {
        GiftCardId giftCardId = EntityId.generate(GiftCardId::new);
        Money amount = new Money(new BigDecimal("50.00"));
        PaymentResultIntegrationEvent event = new PaymentResultIntegrationEvent.PaymentAcceptedIntegrationEvent(
            giftCardId.value().toString(), amount
        );

        ConfirmTopUp result = paymentResult.adapt(event);

        assertThat(result).isNotNull();
        assertThat(result.aggregateId()).isEqualTo(giftCardId);
        assertThat(result.amount()).isEqualTo(amount);
    }

    @Test
    void adaptPaymentRejected_returnsNull() {
        PaymentResultIntegrationEvent event = new PaymentResultIntegrationEvent.PaymentRejectedIntegrationEvent(
            UUID.randomUUID().toString(), new Money(new BigDecimal("10.00")), "declined"
        );

        ConfirmTopUp result = paymentResult.adapt(event);

        assertThat(result).isNull();
    }

    @Test
    void adaptPaymentExpired_returnsNull() {
        PaymentResultIntegrationEvent event = new PaymentResultIntegrationEvent.PaymentExpiredIntegrationEvent(
            UUID.randomUUID().toString(), new Money(new BigDecimal("10.00"))
        );

        ConfirmTopUp result = paymentResult.adapt(event);

        assertThat(result).isNull();
    }

    @Test
    void adaptNullEvent_throwsException() {
        assertThatThrownBy(() -> paymentResult.adapt(null))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
