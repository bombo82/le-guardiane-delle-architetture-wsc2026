package it.giannibombelli.wsc2026.giftcard.application.usecases;

import it.giannibombelli.wsc2026.common.utils.Require;

import it.giannibombelli.wsc2026.common.application.UseCase;
import it.giannibombelli.wsc2026.common.application.events.EventPublisher;
import it.giannibombelli.wsc2026.giftcard.application.commands.RequestGiftCardTopUp;
import it.giannibombelli.wsc2026.giftcard.domain.events.GiftCardEvent;
import it.giannibombelli.wsc2026.giftcard.domain.events.GiftCardTopUpRequested;
import it.giannibombelli.wsc2026.giftcard.domain.giftcard.GiftCard;
import it.giannibombelli.wsc2026.giftcard.domain.giftcard.GiftCardId;
import it.giannibombelli.wsc2026.giftcard.domain.ports.GiftCardRepository;

import static java.util.Objects.requireNonNull;

public final class TopUpRequesting implements UseCase<RequestGiftCardTopUp, GiftCardTopUpRequested> {
    private final GiftCardRepository giftCardRepository;
    private final EventPublisher<GiftCardEvent> eventPublisher;

    public TopUpRequesting(GiftCardRepository giftCardRepository, EventPublisher<GiftCardEvent> eventPublisher) {
        this.giftCardRepository = requireNonNull(giftCardRepository);
        this.eventPublisher = requireNonNull(eventPublisher);
    }

    @Override
    public GiftCardTopUpRequested invoke(RequestGiftCardTopUp cmd) {
        Require.requireArgument(cmd, "command");

        final GiftCardId giftCardId = cmd.aggregateId();
        GiftCard giftCard = giftCardRepository.findById(giftCardId)
            .orElseThrow(() -> new IllegalStateException("Gift card not found: " + giftCardId.value()));

        GiftCardTopUpRequested topUpRequested = giftCard.requestTopUp(cmd.amount());

        giftCardRepository.save(giftCard);
        eventPublisher.publish(topUpRequested);

        return topUpRequested;
    }
}
