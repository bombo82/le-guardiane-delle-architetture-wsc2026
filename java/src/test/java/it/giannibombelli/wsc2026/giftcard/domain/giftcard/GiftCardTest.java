package it.giannibombelli.wsc2026.giftcard.domain.giftcard;

import it.giannibombelli.wsc2026.common.domain.identity.EntityId;
import it.giannibombelli.wsc2026.common.domain.primitive.Description;
import it.giannibombelli.wsc2026.common.domain.primitive.Money;
import it.giannibombelli.wsc2026.giftcard.domain.events.*;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static it.giannibombelli.wsc2026.testsupport.AggregateFactory.createGiftCard;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GiftCardTest {

    @Nested
    class Issuance {
        @Test
        void shouldIssue() {
            GiftCardId id = EntityId.generate(GiftCardId::new);

            GiftCard giftCard = GiftCard.issue(id);

            assertThat(giftCard.id()).isEqualTo(id);
            assertThat(giftCard.balance()).isEqualTo(Money.zero());
        }

        @Test
        void shouldFailIfIdIsNull() {
            assertThatThrownBy(() -> GiftCard.issue(null))
                .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    class TopUpRequest {
        @Test
        void shouldFailIfAmountIsNull() {
            GiftCard giftCard = createGiftCard();

            assertThatThrownBy(() -> giftCard.requestTopUp(null))
                .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void shouldEmitEvent() {
            GiftCard giftCard = createGiftCard();
            Money topUpAmount = new Money(new BigDecimal("25.00"));

            GiftCardTopUpRequested result = giftCard.requestTopUp(topUpAmount);

            assertThat(result.aggregateId()).isEqualTo(giftCard.id());
            assertThat(result.requestedAmount()).isEqualTo(topUpAmount);
        }
    }

    @Nested
    class TopUpConfirmation {
        @Test
        void shouldFailIfAmountIsNull() {
            GiftCard giftCard = createGiftCard();

            assertThatThrownBy(() -> giftCard.confirmTopUp(null))
                .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void shouldIncreaseBalance() {
            Money initialBalance = new Money(new BigDecimal("10.00"));
            GiftCard giftCard = createGiftCard(initialBalance);
            Money topUpAmount = new Money(new BigDecimal("50.00"));

            TopUpConfirmed result = giftCard.confirmTopUp(topUpAmount);

            assertThat(result.aggregateId()).isEqualTo(giftCard.id());
            assertThat(giftCard.balance()).isEqualTo(new Money(new BigDecimal("60.00")));
            assertThat(giftCard.id()).isEqualTo(giftCard.id());
        }
    }

    @Nested
    class Redemption {
        @Test
        void shouldFailIfAmountIsNull() {
            GiftCard giftCard = createGiftCard(new Money(new BigDecimal("100.00")));

            assertThatThrownBy(() -> giftCard.redeem(null))
                .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void shouldSucceedWithSufficientBalance() {
            Money initialBalance = new Money(new BigDecimal("100.00"));
            GiftCard giftCard = createGiftCard(initialBalance);
            Money redeemAmount = new Money(new BigDecimal("37.50"));

            GiftCardRedeemEvents result = giftCard.redeem(redeemAmount);

            assertThat(result).isInstanceOf(GiftCardRedeemEvents.GiftCardRedeemed.class);
            GiftCardRedeemEvents.GiftCardRedeemed redeemed = (GiftCardRedeemEvents.GiftCardRedeemed) result;
            assertThat(redeemed.aggregateId()).isEqualTo(giftCard.id());
            assertThat(redeemed.redeemedAmount()).isEqualTo(redeemAmount);
            assertThat(giftCard.balance()).isEqualTo(new Money(new BigDecimal("62.50")));
            assertThat(giftCard.id()).isEqualTo(giftCard.id());
        }

        @Test
        void shouldFailWithInsufficientBalance() {
            final Money initialAmount = new Money(new BigDecimal("20.00"));
            GiftCard giftCard = createGiftCard(initialAmount);
            Money bigRedeem = new Money(new BigDecimal("50.00"));

            GiftCardRedeemEvents result = giftCard.redeem(bigRedeem);

            assertThat(result).isInstanceOf(GiftCardRedeemEvents.GiftCardNotRedeemed.class);
            GiftCardRedeemEvents.GiftCardNotRedeemed notRedeemed = (GiftCardRedeemEvents.GiftCardNotRedeemed) result;
            assertThat(notRedeemed.aggregateId()).isEqualTo(giftCard.id());
            assertThat(notRedeemed.attemptedAmount()).isEqualTo(bigRedeem);
            assertThat(notRedeemed.reason()).isEqualTo(new Description("insufficient balance"));
            assertThat(giftCard.balance()).isEqualTo(initialAmount);
        }
    }

    @Nested
    class Refunding {
        @Test
        void shouldFailIfAmountIsNull() {
            GiftCard giftCard = createGiftCard(new Money(new BigDecimal("60.00")));

            assertThatThrownBy(() -> giftCard.refund(null))
                .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void shouldRestoreBalance() {
            GiftCard giftCard = createGiftCard(new Money(new BigDecimal("60.00")));
            Money refundAmount = new Money(new BigDecimal("25.00"));

            GiftCardRefunded result = giftCard.refund(refundAmount);

            assertThat(result).isInstanceOf(GiftCardRefunded.class);
            assertThat(result.aggregateId()).isEqualTo(giftCard.id());
            assertThat(result.refundedAmount()).isEqualTo(refundAmount);
            assertThat(giftCard.balance()).isEqualTo(new Money(new BigDecimal("85.00")));
            assertThat(giftCard.id()).isEqualTo(giftCard.id());
        }
    }

    @Nested
    class Crediting {
        @Test
        void shouldFailIfAmountIsNull() {
            GiftCard giftCard = createGiftCard(new Money(new BigDecimal("35.00")));

            assertThatThrownBy(() -> giftCard.credit(null))
                .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void shouldIncreaseBalance() {
            Money initialBalance = new Money(new BigDecimal("35.00"));
            GiftCard giftCard = createGiftCard(initialBalance);
            Money creditAmount = new Money(new BigDecimal("10.00"));

            GiftCardCredited result = giftCard.credit(creditAmount);

            assertThat(result.aggregateId()).isEqualTo(giftCard.id());
            assertThat(result.creditedAmount()).isEqualTo(creditAmount);
            assertThat(giftCard.balance()).isEqualTo(new Money(new BigDecimal("45.00")));
            assertThat(giftCard.id()).isEqualTo(giftCard.id());
        }
    }
}
