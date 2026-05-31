package it.giannibombelli.wsc2026.giftcard.application.usecases;

import it.giannibombelli.wsc2026.common.domain.primitive.Money;
import it.giannibombelli.wsc2026.giftcard.application.commands.RedeemGiftCard;
import it.giannibombelli.wsc2026.giftcard.domain.giftcard.GiftCard;
import it.giannibombelli.wsc2026.giftcard.domain.ports.GiftCardRepository;
import it.giannibombelli.wsc2026.giftcard.infrastructure.SqliteGiftCardRepository;
import it.giannibombelli.wsc2026.testsupport.DatabaseSetup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.util.Optional;

import static it.giannibombelli.wsc2026.testsupport.AggregateFactory.getSavedGiftCard;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GiftCardRedeemingTest {
    private GiftCardRepository repository;
    private GiftCardRedeeming redemption;

    @BeforeEach
    void setUp() {
        final DataSource dataSource = DatabaseSetup.initializeInMemoryDb("giftcard");
        repository = new SqliteGiftCardRepository(dataSource);
        redemption = new GiftCardRedeeming(repository);
    }

    @Nested
    class Construction {
        @Test
        void rejectNullRepository() {
            assertThatThrownBy(() -> new GiftCardRedeeming(null))
                .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    class Execution {
        @Test
        void rejectNullCommand() {
            assertThatThrownBy(() -> redemption.invoke(null))
                .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void shouldPersistWithSufficientBalance() {
            GiftCard giftCard = getSavedGiftCard(repository, new Money(new BigDecimal("100.00")));
            redemption.invoke(new RedeemGiftCard(giftCard.id(), new Money(new BigDecimal("42.00"))));

            GiftCard persisted = repository.findById(giftCard.id()).orElseThrow();

            assertThat(persisted.balance()).isEqualTo(new Money(new BigDecimal("58.00")));
        }

        @Test
        void shouldNotPersistWithInsufficientBalance() {
            GiftCard original = getSavedGiftCard(repository, new Money(new BigDecimal("20.00")));

            redemption.invoke(new RedeemGiftCard(original.id(), new Money(new BigDecimal("50.00"))));

            final Optional<GiftCard> persisted = repository.findById(original.id());
            assertThat(persisted).isPresent();
            assertThat(persisted.get().balance()).isEqualTo(new Money(new BigDecimal("20.00")));
        }
    }
}
