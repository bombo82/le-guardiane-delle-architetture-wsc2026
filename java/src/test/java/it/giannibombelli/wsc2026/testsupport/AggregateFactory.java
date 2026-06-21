package it.giannibombelli.wsc2026.testsupport;

import it.giannibombelli.wsc2026.booking.domain.booking.Booking;
import it.giannibombelli.wsc2026.booking.domain.booking.BookingId;
import it.giannibombelli.wsc2026.booking.domain.primitive.GiftCardReference;
import it.giannibombelli.wsc2026.common.domain.identity.EntityId;
import it.giannibombelli.wsc2026.common.domain.primitive.ClientReference;
import it.giannibombelli.wsc2026.common.domain.primitive.Description;
import it.giannibombelli.wsc2026.common.domain.primitive.Money;
import it.giannibombelli.wsc2026.common.domain.primitive.Timestamp;
import it.giannibombelli.wsc2026.giftcard.domain.giftcard.GiftCard;
import it.giannibombelli.wsc2026.giftcard.domain.giftcard.GiftCardId;
import it.giannibombelli.wsc2026.giftcard.domain.ports.GiftCardRepository;
import it.giannibombelli.wsc2026.payment.domain.payment.Payment;
import it.giannibombelli.wsc2026.payment.domain.payment.PaymentId;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class AggregateFactory {
    public static Booking createBooking() {
        BookingId bookingId = EntityId.generate(BookingId::new);
        GiftCardReference giftCardReference = new GiftCardReference(UUID.randomUUID());
        return Booking.place(bookingId, new Description("Test booking"), giftCardReference);
    }

    public static GiftCard createGiftCard() {
        return createGiftCard(Money.zero());
    }

    public static GiftCard createGiftCard(Money amount) {
        GiftCardId cardId = EntityId.generate(GiftCardId::new);
        GiftCard giftCard = GiftCard.issue(cardId);
        if (amount.isPositive()) {
            giftCard.confirmTopUp(amount);
        }

        return giftCard;
    }

    public static Payment createPayment() {
        String clientReference = UUID.randomUUID().toString();
        Money amount = new Money(new BigDecimal("50.00"));
        return createPayment(clientReference, amount);
    }

    public static Payment createPayment(String clientReference, Money amount) {
        PaymentId paymentId = EntityId.generate(PaymentId::new);
        return Payment.request(paymentId, new ClientReference(UUID.fromString(clientReference)), amount, new Timestamp(Instant.now()));
    }

    public static GiftCard getSavedGiftCard(GiftCardRepository giftCardRepository) {
        return getSavedGiftCard(giftCardRepository, Money.zero());
    }

    public static GiftCard getSavedGiftCard(GiftCardRepository giftCardRepository, Money amount) {
        final GiftCard giftCard = createGiftCard(amount);
        giftCardRepository.save(giftCard);
        return giftCard;
    }
}
