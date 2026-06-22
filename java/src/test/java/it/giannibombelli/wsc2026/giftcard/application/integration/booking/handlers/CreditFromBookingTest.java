package it.giannibombelli.wsc2026.giftcard.application.integration.booking.handlers;

import it.giannibombelli.wsc2026.booking.domain.booking.Booking;
import it.giannibombelli.wsc2026.booking.integration.giftcard.BookingResultIntegrationEvent;
import it.giannibombelli.wsc2026.common.domain.identity.EntityId;
import it.giannibombelli.wsc2026.common.domain.primitive.Money;
import it.giannibombelli.wsc2026.giftcard.application.integration.booking.adapter.BookingResult;
import it.giannibombelli.wsc2026.giftcard.application.usecases.GiftCardCrediting;
import it.giannibombelli.wsc2026.giftcard.domain.giftcard.GiftCard;
import it.giannibombelli.wsc2026.giftcard.domain.giftcard.GiftCardId;
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

class CreditFromBookingTest {

    private GiftCardRepository repository;
    private CreditFromBooking creditFromBooking;

    @BeforeEach
    void setUp() {
        final DataSource dataSource = DatabaseSetup.initializeInMemoryDb("giftcard");
        repository = new SqliteGiftCardRepository(dataSource);

        BookingResult bookingResult = new BookingResult();
        GiftCardCrediting useCase = new GiftCardCrediting(repository);
        creditFromBooking = new CreditFromBooking(bookingResult, useCase);
    }

    @Nested
    class Construction {
        @Test
        void rejectNullParameters() {
            BookingResult bookingResult = new BookingResult();
            GiftCardCrediting useCase = new GiftCardCrediting(repository);

            assertThatThrownBy(() -> new CreditFromBooking(null, useCase))
                .isInstanceOf(NullPointerException.class);

            assertThatThrownBy(() -> new CreditFromBooking(bookingResult, null))
                .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    class BookingResultsHandling {
        @Test
        void onConfirmed_shouldIncreaseBalance() {
            GiftCard giftCard = getSavedGiftCard(repository);
            Booking booking = createBooking();
            Money credit = new Money(new BigDecimal("14.00"));

            creditFromBooking.handle(
                new BookingResultIntegrationEvent.BookingCompletedIntegrationEvent(giftCard.id().value(), credit)
            );

            Optional<GiftCard> updated = repository.findById(giftCard.id());
            assertThat(updated).isPresent();
            assertThat(updated.get().balance()).isEqualTo(credit);
        }

        @Test
        void onRefused_shouldIncreaseBalance() {
            GiftCard giftCard = getSavedGiftCard(repository);
            final Booking booking = createBooking();
            Money credit = new Money(new BigDecimal("9.99"));

            creditFromBooking.handle(
                new BookingResultIntegrationEvent.BookingRefusedIntegrationEvent(giftCard.id().value(), credit)
            );

            GiftCard after = repository.findById(giftCard.id()).orElseThrow();
            assertThat(after.balance()).isEqualTo(credit);
        }

        @Test
        void onRejected_shouldDoNothing() {
            GiftCard giftCard = getSavedGiftCard(repository);
            Booking booking = createBooking();
            Money amount = new Money(new BigDecimal("5.00"));

            creditFromBooking.handle(
                new BookingResultIntegrationEvent.BookingRejectedIntegrationEvent(giftCard.id().value(), amount)
            );

            GiftCard persisted = repository.findById(giftCard.id()).orElseThrow();
            assertThat(persisted.balance()).isEqualTo(Money.zero());
        }

        @Test
        void shouldFailIfCardDoesNotExist() {
            GiftCardId nonExisting = EntityId.generate(GiftCardId::new);
            Booking booking = createBooking();
            Money amount = new Money(new BigDecimal("5.00"));

            assertThatThrownBy(() -> creditFromBooking.handle(
                new BookingResultIntegrationEvent.BookingCompletedIntegrationEvent(nonExisting.value(), amount)
            ))
                .isInstanceOf(IllegalStateException.class);
        }
    }
}
