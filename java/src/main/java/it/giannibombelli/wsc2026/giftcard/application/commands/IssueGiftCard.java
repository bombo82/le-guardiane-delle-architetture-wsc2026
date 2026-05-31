package it.giannibombelli.wsc2026.giftcard.application.commands;

import it.giannibombelli.wsc2026.common.utils.Require;

import it.giannibombelli.wsc2026.common.application.Command;
import it.giannibombelli.wsc2026.giftcard.domain.giftcard.GiftCardId;

public record IssueGiftCard(GiftCardId aggregateId) implements Command<GiftCardId> {
    public IssueGiftCard {
        Require.requireArgument(aggregateId, "giftCardId");
    }
}
