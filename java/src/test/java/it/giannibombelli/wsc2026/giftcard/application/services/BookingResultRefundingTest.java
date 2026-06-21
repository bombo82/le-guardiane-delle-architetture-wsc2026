package it.giannibombelli.wsc2026.giftcard.application.services;

import it.giannibombelli.wsc2026.booking.domain.booking.Booking;
import it.giannibombelli.wsc2026.booking.domain.events.BookingResultEvents;
import it.giannibombelli.wsc2026.common.domain.identity.EntityId;
import it.giannibombelli.wsc2026.common.domain.primitive.Money;
import it.giannibombelli.wsc2026.giftcard.application.usecases.GiftCardRefunding;
import it.giannibombelli.wsc2026.giftcard.domain.giftcard.GiftCard;
import it.giannibombelli.wsc2026.giftcard.domain.giftcard.GiftCardId;
import it.giannibombelli.wsc2026.giftcard.application.policies.RefundGiftCardPolicy;
import it.giannibombelli.wsc2026.giftcard.domain.ports.GiftCardRepository;
import it.giannibombelli.wsc2026.giftcard.infrastructure.SqliteGiftCardRepository;
import it.giannibombelli.wsc2026.testsupport.DatabaseSetup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.util.Optional;

import static it.giannibombelli.wsc2026.testsupport.AggregateFactory.createBooking;
import static it.giannibombelli.wsc2026.testsupport.AggregateFactory.getSavedGiftCard;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BookingResultRefundingTest {

    private GiftCardRepository repository;
    private BookingResultRefunding refundGiftCardService;

    @BeforeEach
    void setUp() {
        final DataSource dataSource = DatabaseSetup.initializeInMemoryDb("giftcard");
        repository = new SqliteGiftCardRepository(dataSource);

        RefundGiftCardPolicy policy = new RefundGiftCardPolicy();
        GiftCardRefunding useCase = new GiftCardRefunding(repository);
        refundGiftCardService = new BookingResultRefunding(policy, useCase);
    }

    @Nested
    class Construction {
        @Test
        void rejectNullParameters() {
            RefundGiftCardPolicy policy = new RefundGiftCardPolicy();
            GiftCardRefunding useCase = new GiftCardRefunding(repository);

            assertThatThrownBy(() -> new BookingResultRefunding(null, useCase))
                .isInstanceOf(NullPointerException.class);

            assertThatThrownBy(() -> new BookingResultRefunding(policy, null))
                .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    class BookingResultsHandling {
        @Test
        void onRejected_shouldRestoreBalance() {
            GiftCard card = getSavedGiftCard(repository);
            Booking booking = createBooking();
            Money refundAmount = new Money(new BigDecimal("20.00"));

            refundGiftCardService.handleBookingResults(
                new BookingResultEvents.BookingRejected(booking.id(), card.id().value(), refundAmount)
            );

            Optional<GiftCard> after = repository.findById(card.id());
            assertThat(after).isPresent();
            assertThat(after.get().balance()).isEqualTo(refundAmount);
        }

        @Test
        void onConfirmed_shouldDoNothing() {
            GiftCard card = getSavedGiftCard(repository);
            Booking booking = createBooking();
            Money amount = new Money(new BigDecimal("10.00"));

            refundGiftCardService.handleBookingResults(
                new BookingResultEvents.BookingConfirmed(booking.id(), card.id().value(), amount)
            );

            GiftCard persisted = repository.findById(card.id()).orElseThrow();
            assertThat(persisted.balance()).isEqualTo(Money.zero());
        }

        @Test
        void shouldFailIfCardDoesNotExist() {
            GiftCardId nonExisting = EntityId.generate(GiftCardId::new);
            Booking booking = createBooking();
            Money amount = new Money(new BigDecimal("10.00"));

            assertThatThrownBy(() -> refundGiftCardService.handleBookingResults(
                new BookingResultEvents.BookingRejected(booking.id(), nonExisting.value(), amount)
            ))
                .isInstanceOf(IllegalStateException.class);
        }
    }
}
