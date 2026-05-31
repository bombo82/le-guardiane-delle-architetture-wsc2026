package it.giannibombelli.wsc2026.giftcard.application.usecases;

import it.giannibombelli.wsc2026.common.utils.Require;

import it.giannibombelli.wsc2026.common.application.UseCase;
import it.giannibombelli.wsc2026.giftcard.application.commands.CreditGiftCard;
import it.giannibombelli.wsc2026.giftcard.domain.events.GiftCardCredited;
import it.giannibombelli.wsc2026.giftcard.domain.giftcard.GiftCard;
import it.giannibombelli.wsc2026.giftcard.domain.giftcard.GiftCardId;
import it.giannibombelli.wsc2026.giftcard.domain.ports.GiftCardRepository;

import static java.util.Objects.requireNonNull;

public class GiftCardCrediting implements UseCase<CreditGiftCard, GiftCardCredited> {
    private final GiftCardRepository giftCardRepository;

    public GiftCardCrediting(GiftCardRepository giftCardRepository) {
        this.giftCardRepository = requireNonNull(giftCardRepository);
    }

    @Override
    public GiftCardCredited invoke(CreditGiftCard cmd) {
        Require.requireArgument(cmd, "command");
        GiftCardId giftCardId = cmd.aggregateId();

        GiftCard giftCard = giftCardRepository.findById(giftCardId)
            .orElseThrow(() -> new IllegalStateException("GiftCard not found for credit: " + giftCardId));

        GiftCardCredited credited = giftCard.credit(cmd.amount());

        giftCardRepository.save(giftCard);
        return credited;
    }
}
