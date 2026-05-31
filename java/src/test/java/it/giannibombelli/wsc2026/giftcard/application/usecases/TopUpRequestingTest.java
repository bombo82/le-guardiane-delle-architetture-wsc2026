package it.giannibombelli.wsc2026.giftcard.application.usecases;

import it.giannibombelli.wsc2026.common.domain.identity.EntityId;
import it.giannibombelli.wsc2026.common.domain.primitive.Money;
import it.giannibombelli.wsc2026.giftcard.application.commands.RequestGiftCardTopUp;
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

class TopUpRequestingTest {
    private GiftCardRepository repository;
    private TopUpRequesting topUp;

    @BeforeEach
    void setUp() {
        final DataSource dataSource = DatabaseSetup.initializeInMemoryDb("giftcard");
        repository = new SqliteGiftCardRepository(dataSource);
        topUp = new TopUpRequesting(repository, event -> {
        });
    }

    @Nested
    class Construction {
        @Test
        void rejectNullRepository() {
            assertThatThrownBy(() -> new TopUpRequesting(null, event -> {
            }))
                .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    class Execution {
        @Test
        void rejectNullCommand() {
            assertThatThrownBy(() -> topUp.invoke(null))
                .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void shouldPersistWithoutChangingBalance() {
            var original = AggregateFactory.getSavedGiftCard(repository);
            Money requested = new Money(new BigDecimal("42.00"));
            topUp.invoke(new RequestGiftCardTopUp(original.id(), requested));

            var persisted = repository.findById(original.id());

            assertThat(persisted).isPresent();
            assertThat(persisted.get().balance()).isEqualTo(Money.zero());
        }

        @Test
        void shouldFailIfCardDoesNotExist() {
            GiftCardId nonExisting = EntityId.generate(GiftCardId::new);
            RequestGiftCardTopUp cmd = new RequestGiftCardTopUp(nonExisting, new Money(new BigDecimal("10.00")));

            assertThatThrownBy(() -> topUp.invoke(cmd))
                .isInstanceOf(IllegalStateException.class);
        }
    }
}
