package it.giannibombelli.wsc2026.giftcard.application.integration.booking.adapter;

import it.giannibombelli.wsc2026.booking.integration.giftcard.BookingResultIntegrationEvent;
import it.giannibombelli.wsc2026.common.utils.Require;
import it.giannibombelli.wsc2026.giftcard.application.commands.CreditGiftCard;
import it.giannibombelli.wsc2026.giftcard.application.commands.RefundGiftCard;
import it.giannibombelli.wsc2026.giftcard.domain.giftcard.GiftCardId;

/**
 * Anti-Corruption Layer che traduce la Published Language esposta da {@code booking}
 * nei command interni del BC {@code giftcard}.
 * <p>
 * Isola il modello di {@code giftcard} da eventuali evoluzioni del modello interno di {@code booking}:
 * l'unico contratto condiviso è {@link BookingResultIntegrationEvent}.
 */
public final class BookingResult {

    public CreditGiftCard adapt(BookingResultIntegrationEvent event) {
        Require.requireArgument(event, "Booking result integration event");
        return switch (event) {
            case BookingResultIntegrationEvent.BookingCompletedIntegrationEvent it ->
                new CreditGiftCard(new GiftCardId(it.giftCardReference()), it.amount());
            case BookingResultIntegrationEvent.BookingRefusedIntegrationEvent it ->
                new CreditGiftCard(new GiftCardId(it.giftCardReference()), it.amount());
            case BookingResultIntegrationEvent.BookingRejectedIntegrationEvent ignored -> null;
        };
    }

    public RefundGiftCard adaptRejected(BookingResultIntegrationEvent.BookingRejectedIntegrationEvent event) {
        Require.requireArgument(event, "Booking rejected integration event");
        return new RefundGiftCard(new GiftCardId(event.giftCardReference()), event.amount());
    }
}
