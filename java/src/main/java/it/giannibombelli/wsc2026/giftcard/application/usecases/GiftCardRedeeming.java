package it.giannibombelli.wsc2026.giftcard.application.usecases;

import it.giannibombelli.wsc2026.common.utils.Require;

import it.giannibombelli.wsc2026.common.application.UseCase;
import it.giannibombelli.wsc2026.giftcard.application.commands.RedeemGiftCard;
import it.giannibombelli.wsc2026.giftcard.domain.events.GiftCardRedeemEvents;
import it.giannibombelli.wsc2026.giftcard.domain.giftcard.GiftCard;
import it.giannibombelli.wsc2026.giftcard.domain.giftcard.GiftCardId;
import it.giannibombelli.wsc2026.giftcard.domain.ports.GiftCardRepository;

import static java.util.Objects.requireNonNull;

public final class GiftCardRedeeming implements UseCase<RedeemGiftCard, GiftCardRedeemEvents> {
    private final GiftCardRepository giftCardRepository;

    public GiftCardRedeeming(GiftCardRepository giftCardRepository) {
        this.giftCardRepository = requireNonNull(giftCardRepository);
    }

    @Override
    public GiftCardRedeemEvents invoke(RedeemGiftCard cmd) {
        Require.requireArgument(cmd, "command");
        GiftCardId giftCardId = cmd.aggregateId();

        GiftCard giftCard = giftCardRepository.findById(giftCardId)
            .orElseThrow(() -> new IllegalStateException("GiftCard not found for redeem: " + giftCardId));

        GiftCardRedeemEvents redeemResult = giftCard.redeem(cmd.amount());

        giftCardRepository.save(giftCard);
        return redeemResult;
    }
}
