package it.giannibombelli.wsc2026.giftcard.domain.events;

import it.giannibombelli.wsc2026.common.utils.Require;

import it.giannibombelli.wsc2026.common.domain.primitive.Money;
import it.giannibombelli.wsc2026.giftcard.domain.giftcard.GiftCardId;

public record GiftCardRefunded(
    GiftCardId aggregateId,
    Money refundedAmount
) implements GiftCardEvent {
    public GiftCardRefunded {
        Require.requireArgument(aggregateId, "giftCardId");
        Require.requireArgument(refundedAmount, "refundedAmount");
    }
}
