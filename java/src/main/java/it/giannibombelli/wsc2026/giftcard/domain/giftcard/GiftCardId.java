package it.giannibombelli.wsc2026.giftcard.domain.giftcard;

import it.giannibombelli.wsc2026.common.utils.Require;

import it.giannibombelli.wsc2026.common.domain.identity.AggregateId;

import java.util.UUID;

public record GiftCardId(UUID value) implements AggregateId {
    public GiftCardId {
        Require.requireArgument(value, "GiftCardId value");
    }
}
