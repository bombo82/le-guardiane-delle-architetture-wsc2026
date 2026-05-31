package it.giannibombelli.wsc2026.giftcard.domain.events;

import it.giannibombelli.wsc2026.common.utils.Require;

import it.giannibombelli.wsc2026.common.domain.primitive.Money;
import it.giannibombelli.wsc2026.giftcard.domain.giftcard.GiftCardId;

public record GiftCardTopUpRequested(
    GiftCardId aggregateId,
    Money requestedAmount,
    Money currentBalance
) implements GiftCardEvent {
    public GiftCardTopUpRequested {
        Require.requireArgument(aggregateId, "giftCardId");
        Require.requireArgument(requestedAmount, "requestedAmount");
        Require.requireArgument(currentBalance, "currentBalance");
    }
}
