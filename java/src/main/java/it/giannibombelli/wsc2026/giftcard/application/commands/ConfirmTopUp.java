package it.giannibombelli.wsc2026.giftcard.application.commands;

import it.giannibombelli.wsc2026.common.utils.Require;

import it.giannibombelli.wsc2026.common.application.Command;
import it.giannibombelli.wsc2026.common.domain.primitive.Money;
import it.giannibombelli.wsc2026.giftcard.domain.giftcard.GiftCardId;

public record ConfirmTopUp(GiftCardId aggregateId, Money amount) implements Command<GiftCardId> {
    public ConfirmTopUp {
        Require.requireArgument(aggregateId, "giftCardId");
        Require.requireArgument(amount, "amount");
    }
}
