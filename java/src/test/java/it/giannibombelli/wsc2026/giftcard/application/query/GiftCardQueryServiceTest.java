package it.giannibombelli.wsc2026.giftcard.application.query;

import it.giannibombelli.wsc2026.common.domain.identity.EntityId;
import it.giannibombelli.wsc2026.common.domain.primitive.Money;
import it.giannibombelli.wsc2026.giftcard.domain.giftcard.GiftCard;
import it.giannibombelli.wsc2026.giftcard.domain.giftcard.GiftCardId;
import it.giannibombelli.wsc2026.giftcard.domain.ports.GiftCardRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class GiftCardQueryServiceTest {

    @Test
    void shouldReturnGiftCardDetailsWhenGiftCardExists() {
        InMemoryGiftCardRepository repository = new InMemoryGiftCardRepository();
        GiftCardQueryService queryService = new GiftCardQueryService(repository);
        GiftCardId cardId = EntityId.generate(GiftCardId::new);
        Money balance = new Money(new BigDecimal("42.50"));
        GiftCard card = new GiftCard(cardId, balance);
        repository.save(card);

        Optional<GiftCardDetails> result = queryService.findById(cardId);

        assertThat(result).isPresent();
        assertThat(result.get().id()).isEqualTo(cardId.value());
        assertThat(result.get().balance()).isEqualTo(balance);
    }

    @Test
    void shouldReturnEmptyWhenGiftCardDoesNotExist() {
        InMemoryGiftCardRepository repository = new InMemoryGiftCardRepository();
        GiftCardQueryService queryService = new GiftCardQueryService(repository);
        GiftCardId cardId = EntityId.generate(GiftCardId::new);

        Optional<GiftCardDetails> result = queryService.findById(cardId);

        assertThat(result).isEmpty();
    }

    private static final class InMemoryGiftCardRepository implements GiftCardRepository {
        private final Map<UUID, GiftCard> cards = new HashMap<>();

        @Override
        public void save(GiftCard card) {
            cards.put(card.id().value(), card);
        }

        @Override
        public Optional<GiftCard> findById(GiftCardId id) {
            return Optional.ofNullable(cards.get(id.value()));
        }
    }
}
