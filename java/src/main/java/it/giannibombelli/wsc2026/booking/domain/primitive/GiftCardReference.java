package it.giannibombelli.wsc2026.booking.domain.primitive;

import it.giannibombelli.wsc2026.common.utils.Require;

import java.util.UUID;

/**
 * Riferimento opaco a una gift card gestita dal BC {@code giftcard}.
 * Il BC {@code booking} non conosce la struttura interna di {@code giftcard.GiftCardId}:
 * utilizza solo questo value object per indicare a quale gift card si riferisce una prenotazione.
 */
public record GiftCardReference(UUID value) {
    public GiftCardReference {
        Require.requireArgument(value, "value");
    }
}
