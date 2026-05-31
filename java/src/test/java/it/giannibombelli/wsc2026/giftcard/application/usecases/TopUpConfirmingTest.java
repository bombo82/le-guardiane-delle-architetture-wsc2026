package it.giannibombelli.wsc2026.giftcard.application.usecases;

import it.giannibombelli.wsc2026.common.domain.identity.EntityId;
import it.giannibombelli.wsc2026.common.domain.primitive.Money;
import it.giannibombelli.wsc2026.giftcard.application.commands.ConfirmTopUp;
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

class TopUpConfirmingTest {
    private GiftCardRepository repository;
    private TopUpConfirming confirming;

    @BeforeEach
    void setUp() {
        final DataSource dataSource = DatabaseSetup.initializeInMemoryDb("giftcard");
        repository = new SqliteGiftCardRepository(dataSource);
        confirming = new TopUpConfirming(repository);
    }

    @Nested
    class Construction {
        @Test
        void rejectNullRepository() {
            assertThatThrownBy(() -> new TopUpConfirming(null))
                .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    class Execution {
        @Test
        void rejectNullCommand() {
            assertThatThrownBy(() -> confirming.invoke(null))
                .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void shouldPersistTopUpConfirmation() {
            GiftCard giftCard = AggregateFactory.getSavedGiftCard(repository);
            Money topUpAmount = new Money(new BigDecimal("33.00"));

            confirming.invoke(new ConfirmTopUp(giftCard.id(), topUpAmount));

            GiftCard persisted = repository.findById(giftCard.id()).orElseThrow();
            assertThat(persisted.balance()).isEqualTo(topUpAmount);
        }

        @Test
        void shouldFailIfCardDoesNotExist() {
            GiftCardId nonExisting = EntityId.generate(GiftCardId::new);
            Money amount = new Money(new BigDecimal("10.00"));

            assertThatThrownBy(() -> confirming.invoke(new ConfirmTopUp(nonExisting, amount)))
                .isInstanceOf(IllegalStateException.class);
        }
    }
}
