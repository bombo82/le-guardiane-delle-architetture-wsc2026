package it.giannibombelli.wsc2026.giftcard.application.query;

import it.giannibombelli.wsc2026.common.domain.primitive.Money;

import java.util.UUID;

public record GiftCardDetails(UUID id, Money balance) {
}
