package it.giannibombelli.wsc2026.giftcard.application.usecases;

import it.giannibombelli.wsc2026.common.utils.Require;

import it.giannibombelli.wsc2026.common.application.UseCase;
import it.giannibombelli.wsc2026.giftcard.application.commands.IssueGiftCard;
import it.giannibombelli.wsc2026.giftcard.domain.events.GiftCardIssued;
import it.giannibombelli.wsc2026.giftcard.domain.giftcard.GiftCard;
import it.giannibombelli.wsc2026.giftcard.domain.ports.GiftCardRepository;

import static java.util.Objects.requireNonNull;

public final class GiftCardIssuing implements UseCase<IssueGiftCard, GiftCardIssued> {
    private final GiftCardRepository giftCardRepository;

    public GiftCardIssuing(GiftCardRepository giftCardRepository) {
        this.giftCardRepository = requireNonNull(giftCardRepository);
    }

    @Override
    public GiftCardIssued invoke(IssueGiftCard cmd) {
        Require.requireArgument(cmd, "command");

        GiftCard card = GiftCard.issue(cmd.aggregateId());

        giftCardRepository.save(card);

        return new GiftCardIssued(card.id(), card.balance());
    }
}
