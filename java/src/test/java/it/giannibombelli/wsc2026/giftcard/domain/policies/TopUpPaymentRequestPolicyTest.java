package it.giannibombelli.wsc2026.giftcard.domain.policies;

import it.giannibombelli.wsc2026.common.domain.primitive.ClientReference;
import it.giannibombelli.wsc2026.common.domain.primitive.Money;
import it.giannibombelli.wsc2026.giftcard.domain.events.GiftCardTopUpRequested;
import it.giannibombelli.wsc2026.giftcard.domain.giftcard.GiftCard;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static it.giannibombelli.wsc2026.testsupport.AggregateFactory.createGiftCard;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * La policy è deliberatamente uno stub; il test verifica solo che venga invocata.
 */
class TopUpPaymentRequestPolicyTest {

    @Test
    void evaluate_withNullEvent_throwsIllegalArgumentException() {
        TopUpPaymentRequestPolicy policy = new TopUpPaymentRequestPolicy();

        assertThatThrownBy(() -> policy.evaluate(null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void evaluate_returnsRequestPayment() {
        GiftCard card = createGiftCard();
        GiftCardTopUpRequested event = card.requestTopUp(new Money(new BigDecimal("25.50")));

        TopUpPaymentRequestPolicy policy = new TopUpPaymentRequestPolicy();

        var cmd = policy.evaluate(event);

        org.assertj.core.api.Assertions.assertThat(cmd.clientReference()).isEqualTo(new ClientReference(event.aggregateId().value()));
        org.assertj.core.api.Assertions.assertThat(cmd.amount()).isEqualTo(event.requestedAmount());
    }
}
