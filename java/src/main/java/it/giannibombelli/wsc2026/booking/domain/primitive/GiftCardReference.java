package it.giannibombelli.wsc2026.booking.domain.primitive;

import it.giannibombelli.wsc2026.common.utils.Require;

import java.util.UUID;

/**
 * Riferimento opaco a una gift card: {@code booking} non conosce la struttura interna
 * di {@code giftcard.GiftCardId}.
 */
public record GiftCardReference(UUID value) {
    public GiftCardReference {
        Require.requireArgument(value, "value");
    }
}
