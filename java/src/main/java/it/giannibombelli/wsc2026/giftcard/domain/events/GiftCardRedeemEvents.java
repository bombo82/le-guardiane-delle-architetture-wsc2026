package it.giannibombelli.wsc2026.giftcard.domain.events;

import it.giannibombelli.wsc2026.common.utils.Require;

import it.giannibombelli.wsc2026.common.domain.primitive.Description;
import it.giannibombelli.wsc2026.common.domain.primitive.Money;
import it.giannibombelli.wsc2026.giftcard.domain.giftcard.GiftCardId;

/**
 * Sealed interface so the aggregate can return exactly one of the two possible outcomes.
 */
public sealed interface GiftCardRedeemEvents extends GiftCardEvent
    permits GiftCardRedeemEvents.GiftCardRedeemed, GiftCardRedeemEvents.GiftCardNotRedeemed {

    record GiftCardRedeemed(
        GiftCardId aggregateId,
        Money redeemedAmount
    ) implements GiftCardRedeemEvents {

        public GiftCardRedeemed {
            Require.requireArgument(aggregateId, "giftCardId");
            Require.requireArgument(redeemedAmount, "redeemedAmount");
        }
    }

    record GiftCardNotRedeemed(
        GiftCardId aggregateId,
        Money attemptedAmount,
        Description reason
    ) implements GiftCardRedeemEvents {

        public GiftCardNotRedeemed {
            Require.requireArgument(aggregateId, "giftCardId");
            Require.requireArgument(attemptedAmount, "attemptedAmount");
            Require.requireArgument(reason, "reason");
        }
    }
}
