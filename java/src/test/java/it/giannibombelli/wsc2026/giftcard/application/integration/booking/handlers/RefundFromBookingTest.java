package it.giannibombelli.wsc2026.giftcard.application.integration.booking.handlers;

import it.giannibombelli.wsc2026.booking.domain.booking.Booking;
import it.giannibombelli.wsc2026.booking.integration.giftcard.BookingResultIntegrationEvent;
import it.giannibombelli.wsc2026.common.domain.identity.EntityId;
import it.giannibombelli.wsc2026.common.domain.primitive.Money;
import it.giannibombelli.wsc2026.giftcard.application.integration.booking.adapter.BookingResult;
import it.giannibombelli.wsc2026.giftcard.application.usecases.GiftCardRefunding;
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

class RefundFromBookingTest {

    private GiftCardRepository repository;
    private RefundFromBooking refundFromBooking;

    @BeforeEach
    void setUp() {
        final DataSource dataSource = DatabaseSetup.initializeInMemoryDb("giftcard");
        repository = new SqliteGiftCardRepository(dataSource);

        BookingResult bookingResult = new BookingResult();
        GiftCardRefunding useCase = new GiftCardRefunding(repository);
        refundFromBooking = new RefundFromBooking(bookingResult, useCase);
    }

    @Nested
    class Construction {
        @Test
        void rejectNullParameters() {
            BookingResult bookingResult = new BookingResult();
            GiftCardRefunding useCase = new GiftCardRefunding(repository);

            assertThatThrownBy(() -> new RefundFromBooking(null, useCase))
                .isInstanceOf(NullPointerException.class);

            assertThatThrownBy(() -> new RefundFromBooking(bookingResult, null))
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

            refundFromBooking.handle(
                new BookingResultIntegrationEvent.BookingRejectedIntegrationEvent(card.id().value(), refundAmount)
            );

            Optional<GiftCard> after = repository.findById(card.id());
            assertThat(after).isPresent();
            assertThat(after.get().balance()).isEqualTo(refundAmount);
        }

        @Test
        void shouldFailIfCardDoesNotExist() {
            GiftCardId nonExisting = EntityId.generate(GiftCardId::new);
            Booking booking = createBooking();
            Money amount = new Money(new BigDecimal("10.00"));

            assertThatThrownBy(() -> refundFromBooking.handle(
                new BookingResultIntegrationEvent.BookingRejectedIntegrationEvent(nonExisting.value(), amount)
            ))
                .isInstanceOf(IllegalStateException.class);
        }
    }
}
