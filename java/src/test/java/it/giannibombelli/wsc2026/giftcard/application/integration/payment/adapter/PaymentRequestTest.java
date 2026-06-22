package it.giannibombelli.wsc2026.giftcard.application.integration.payment.adapter;

import it.giannibombelli.wsc2026.common.domain.identity.EntityId;
import it.giannibombelli.wsc2026.common.domain.primitive.Money;
import it.giannibombelli.wsc2026.giftcard.domain.events.GiftCardTopUpRequested;
import it.giannibombelli.wsc2026.giftcard.domain.giftcard.GiftCardId;
import it.giannibombelli.wsc2026.payment.integration.PaymentRequestIntegrationCommand;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentRequestTest {

    @Test
    void fromTopUp_returnsIntegrationCommand() {
        GiftCardId giftCardId = EntityId.generate(GiftCardId::new);
        Money requestedAmount = new Money(new BigDecimal("25.00"));
        Money currentBalance = new Money(new BigDecimal("10.00"));
        GiftCardTopUpRequested event = new GiftCardTopUpRequested(giftCardId, requestedAmount, currentBalance);

        PaymentRequestIntegrationCommand command = PaymentRequest.fromTopUp(event);

        assertThat(command.clientReference()).isEqualTo(giftCardId.value().toString());
        assertThat(command.amount()).isEqualTo(requestedAmount);
    }

    @Test
    void fromTopUp_withNullEvent_throwsException() {
        assertThatThrownBy(() -> PaymentRequest.fromTopUp(null))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
