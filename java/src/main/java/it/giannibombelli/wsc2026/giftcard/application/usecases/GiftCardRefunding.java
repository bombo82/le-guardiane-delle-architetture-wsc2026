package it.giannibombelli.wsc2026.giftcard.application.usecases;

import it.giannibombelli.wsc2026.common.utils.Require;

import it.giannibombelli.wsc2026.common.application.UseCase;
import it.giannibombelli.wsc2026.giftcard.application.commands.RefundGiftCard;
import it.giannibombelli.wsc2026.giftcard.domain.events.GiftCardRefunded;
import it.giannibombelli.wsc2026.giftcard.domain.giftcard.GiftCard;
import it.giannibombelli.wsc2026.giftcard.domain.giftcard.GiftCardId;
import it.giannibombelli.wsc2026.giftcard.domain.ports.GiftCardRepository;

import static java.util.Objects.requireNonNull;

public class GiftCardRefunding implements UseCase<RefundGiftCard, GiftCardRefunded> {
    private final GiftCardRepository giftCardRepository;

    public GiftCardRefunding(GiftCardRepository giftCardRepository) {
        this.giftCardRepository = requireNonNull(giftCardRepository);
    }

    @Override
    public GiftCardRefunded invoke(RefundGiftCard cmd) {
        Require.requireArgument(cmd, "command");
        GiftCardId giftCardId = cmd.aggregateId();

        GiftCard giftCard = giftCardRepository.findById(giftCardId)
            .orElseThrow(() -> new IllegalStateException("GiftCard not found for credit: " + giftCardId));

        GiftCardRefunded refunded = giftCard.refund(cmd.amount());

        giftCardRepository.save(giftCard);
        return refunded;
    }
}
