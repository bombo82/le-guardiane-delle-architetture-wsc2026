package it.giannibombelli.wsc2026.giftcard.infrastructure;

import it.giannibombelli.wsc2026.common.domain.identity.EntityId;
import it.giannibombelli.wsc2026.common.domain.primitive.Money;
import it.giannibombelli.wsc2026.giftcard.domain.giftcard.GiftCard;
import it.giannibombelli.wsc2026.giftcard.domain.giftcard.GiftCardId;
import it.giannibombelli.wsc2026.giftcard.domain.ports.GiftCardRepository;
import it.giannibombelli.wsc2026.testsupport.DatabaseSetup;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifica il contratto del repository senza ripetere le regole di business di dominio.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GiftCardRepositoryTest {
    private GiftCardRepository repository;

    @BeforeAll
    void setUp() {
        final DataSource dataSource = DatabaseSetup.initializeFileDb("giftcard", getClass().getSimpleName());
        repository = new SqliteGiftCardRepository(dataSource);
    }

    @Nested
    class Save {
        @Test
        void shouldPersistNewCard() {
            GiftCardId cardId = EntityId.generate(GiftCardId::new);
            GiftCard original = GiftCard.issue(cardId);

            repository.save(original);
            Optional<GiftCard> reloaded = repository.findById(cardId);

            assertThat(reloaded).isPresent();
            GiftCard found = reloaded.get();
            assertThat(found.id()).isEqualTo(cardId);
            assertThat(found.balance()).isEqualTo(Money.zero());
        }

        @Test
        void shouldUpdateBalance() {
            GiftCardId cardId = EntityId.generate(GiftCardId::new);
            GiftCard credited = new GiftCard(cardId, new Money(new BigDecimal("37.75")));

            repository.save(credited);
            Optional<GiftCard> reloaded = repository.findById(cardId);

            assertThat(reloaded).isPresent();
            assertThat(reloaded.get().balance()).isEqualTo(new Money(new BigDecimal("37.75")));
        }
    }

    @Nested
    class FindById {
        @Test
        void shouldReturnEmptyWhenNotFound() {
            GiftCardId nonExistentId = EntityId.generate(GiftCardId::new);

            Optional<GiftCard> reloaded = repository.findById(nonExistentId);

            assertThat(reloaded).isEmpty();
        }
    }
}
