package it.giannibombelli.wsc2026.giftcard.application.usecases;

import it.giannibombelli.wsc2026.common.utils.Require;

import it.giannibombelli.wsc2026.common.application.UseCase;
import it.giannibombelli.wsc2026.giftcard.application.commands.ConfirmTopUp;
import it.giannibombelli.wsc2026.giftcard.domain.events.TopUpConfirmed;
import it.giannibombelli.wsc2026.giftcard.domain.giftcard.GiftCard;
import it.giannibombelli.wsc2026.giftcard.domain.giftcard.GiftCardId;
import it.giannibombelli.wsc2026.giftcard.domain.ports.GiftCardRepository;

import static java.util.Objects.requireNonNull;

public class TopUpConfirming implements UseCase<ConfirmTopUp, TopUpConfirmed> {
    private final GiftCardRepository giftCardRepository;

    public TopUpConfirming(GiftCardRepository giftCardRepository) {
        this.giftCardRepository = requireNonNull(giftCardRepository);
    }

    @Override
    public TopUpConfirmed invoke(ConfirmTopUp cmd) {
        Require.requireArgument(cmd, "command");
        GiftCardId giftCardId = cmd.aggregateId();

        GiftCard giftCard = giftCardRepository.findById(giftCardId)
            .orElseThrow(() -> new IllegalStateException("GiftCard not found for top-up confirmation: " + giftCardId));

        TopUpConfirmed confirmed = giftCard.confirmTopUp(cmd.amount());

        giftCardRepository.save(giftCard);
        return confirmed;
    }
}
