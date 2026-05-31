package it.giannibombelli.wsc2026.giftcard.domain.events;

import it.giannibombelli.wsc2026.common.utils.Require;

import it.giannibombelli.wsc2026.common.domain.primitive.Money;
import it.giannibombelli.wsc2026.giftcard.domain.giftcard.GiftCardId;

public record GiftCardCredited(
    GiftCardId aggregateId,
    Money creditedAmount
) implements GiftCardEvent {
    public GiftCardCredited {
        Require.requireArgument(aggregateId, "giftCardId");
        Require.requireArgument(creditedAmount, "creditedAmount");
    }
}
