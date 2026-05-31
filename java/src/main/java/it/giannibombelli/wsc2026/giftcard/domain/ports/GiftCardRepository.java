package it.giannibombelli.wsc2026.giftcard.domain.ports;

import it.giannibombelli.wsc2026.giftcard.domain.giftcard.GiftCard;
import it.giannibombelli.wsc2026.giftcard.domain.giftcard.GiftCardId;

import java.util.Optional;

public interface GiftCardRepository {
    void save(GiftCard giftCard);

    Optional<GiftCard> findById(GiftCardId id);
}
