package it.giannibombelli.wsc2026.giftcard.application.commands;

import it.giannibombelli.wsc2026.common.domain.identity.EntityId;
import it.giannibombelli.wsc2026.common.domain.primitive.Money;
import it.giannibombelli.wsc2026.giftcard.domain.giftcard.GiftCardId;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GiftCardCommandsTest {
    @Nested
    class IssueGiftCardValidation {
        @Test
        void validate() {
            assertThatThrownBy(() -> new IssueGiftCard(null))
                .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    class RequestGiftCardTopUpValidation {
        @Test
        void validate() {
            GiftCardId cardId = EntityId.generate(GiftCardId::new);

            assertThatThrownBy(() -> new RequestGiftCardTopUp(null, new Money(BigDecimal.TEN)))
                .isInstanceOf(IllegalArgumentException.class);

            assertThatThrownBy(() -> new RequestGiftCardTopUp(cardId, null))
                .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    class ConfirmTopUpValidation {
        @Test
        void validate() {
            GiftCardId cardId = EntityId.generate(GiftCardId::new);

            assertThatThrownBy(() -> new ConfirmTopUp(null, new Money(BigDecimal.TEN)))
                .isInstanceOf(IllegalArgumentException.class);

            assertThatThrownBy(() -> new ConfirmTopUp(cardId, null))
                .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    class RedeemGiftCardValidation {
        @Test
        void validate() {
            GiftCardId cardId = EntityId.generate(GiftCardId::new);

            assertThatThrownBy(() -> new RedeemGiftCard(null, new Money(BigDecimal.TEN)))
                .isInstanceOf(IllegalArgumentException.class);

            assertThatThrownBy(() -> new RedeemGiftCard(cardId, null))
                .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    class RefundGiftCardValidation {
        @Test
        void validate() {
            GiftCardId cardId = EntityId.generate(GiftCardId::new);

            assertThatThrownBy(() -> new RefundGiftCard(null, new Money(BigDecimal.TEN)))
                .isInstanceOf(IllegalArgumentException.class);

            assertThatThrownBy(() -> new RefundGiftCard(cardId, null))
                .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    class CreditGiftCardValidation {
        @Test
        void validate() {
            GiftCardId cardId = EntityId.generate(GiftCardId::new);

            assertThatThrownBy(() -> new CreditGiftCard(null, new Money(BigDecimal.TEN)))
                .isInstanceOf(IllegalArgumentException.class);

            assertThatThrownBy(() -> new CreditGiftCard(cardId, null))
                .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
