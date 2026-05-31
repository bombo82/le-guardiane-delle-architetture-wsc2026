package it.giannibombelli.wsc2026.giftcard.application.usecases;

import it.giannibombelli.wsc2026.common.domain.identity.EntityId;
import it.giannibombelli.wsc2026.common.domain.primitive.Money;
import it.giannibombelli.wsc2026.giftcard.application.commands.CreditGiftCard;
import it.giannibombelli.wsc2026.giftcard.domain.giftcard.GiftCard;
import it.giannibombelli.wsc2026.giftcard.domain.giftcard.GiftCardId;
import it.giannibombelli.wsc2026.giftcard.domain.ports.GiftCardRepository;
import it.giannibombelli.wsc2026.giftcard.infrastructure.SqliteGiftCardRepository;
import it.giannibombelli.wsc2026.testsupport.AggregateFactory;
import it.giannibombelli.wsc2026.testsupport.DatabaseSetup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GiftCardCreditingTest {
    private GiftCardRepository repository;
    private GiftCardCrediting crediting;

    @BeforeEach
    void setUp() {
        final DataSource dataSource = DatabaseSetup.initializeInMemoryDb("giftcard");
        repository = new SqliteGiftCardRepository(dataSource);
        crediting = new GiftCardCrediting(repository);
    }

    @Nested
    class Construction {
        @Test
        void rejectNullRepository() {
            assertThatThrownBy(() -> new GiftCardCrediting(null))
                .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    class Execution {
        @Test
        void rejectNullCommand() {
            assertThatThrownBy(() -> crediting.invoke(null))
                .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void shouldPersistCredit() {
            GiftCard giftCard = AggregateFactory.getSavedGiftCard(repository);
            Money creditAmount = new Money(new BigDecimal("25.00"));

            crediting.invoke(new CreditGiftCard(giftCard.id(), creditAmount));

            GiftCard persisted = repository.findById(giftCard.id()).orElseThrow();
            assertThat(persisted.balance()).isEqualTo(creditAmount);
        }

        @Test
        void shouldFailIfCardDoesNotExist() {
            GiftCardId nonExisting = EntityId.generate(GiftCardId::new);
            Money amount = new Money(new BigDecimal("10.00"));

            assertThatThrownBy(() -> crediting.invoke(new CreditGiftCard(nonExisting, amount)))
                .isInstanceOf(IllegalStateException.class);
        }
    }
}
