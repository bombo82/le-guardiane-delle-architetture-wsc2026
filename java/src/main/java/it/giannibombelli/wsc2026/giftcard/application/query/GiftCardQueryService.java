package it.giannibombelli.wsc2026.giftcard.application.query;

import it.giannibombelli.wsc2026.giftcard.domain.giftcard.GiftCard;
import it.giannibombelli.wsc2026.giftcard.domain.giftcard.GiftCardId;
import it.giannibombelli.wsc2026.giftcard.domain.ports.GiftCardRepository;

import java.util.Optional;

public final class GiftCardQueryService {
    private final GiftCardRepository repository;

    public GiftCardQueryService(GiftCardRepository repository) {
        this.repository = repository;
    }

    public Optional<GiftCardDetails> findById(GiftCardId id) {
        return repository.findById(id)
            .map(this::toDetails);
    }

    private GiftCardDetails toDetails(GiftCard card) {
        return new GiftCardDetails(
            card.id().value(),
            card.balance()
        );
    }
}
