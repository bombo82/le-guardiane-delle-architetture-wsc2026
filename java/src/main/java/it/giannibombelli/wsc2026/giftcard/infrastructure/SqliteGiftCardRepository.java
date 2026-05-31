package it.giannibombelli.wsc2026.giftcard.infrastructure;

import it.giannibombelli.wsc2026.common.domain.primitive.Money;
import it.giannibombelli.wsc2026.giftcard.domain.giftcard.GiftCard;
import it.giannibombelli.wsc2026.giftcard.domain.giftcard.GiftCardId;
import it.giannibombelli.wsc2026.giftcard.domain.ports.GiftCardRepository;
import org.jooq.DSLContext;
import org.jooq.Record2;
import org.jooq.Result;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.table;

public final class SqliteGiftCardRepository implements GiftCardRepository {
    private static final org.jooq.Table<?> GIFT_CARD = table("gift_card");
    private static final org.jooq.Field<String> ID = field("id", String.class);
    private static final org.jooq.Field<BigDecimal> BALANCE = field("balance", BigDecimal.class);

    private final DSLContext dsl;

    public SqliteGiftCardRepository(DataSource dataSource) {
        this.dsl = DSL.using(dataSource, SQLDialect.SQLITE);
    }

    @Override
    public void save(GiftCard giftCard) {
        String idStr = giftCard.id().value().toString();
        BigDecimal bal = giftCard.balance().value();

        // Upsert (PK exists → update; else insert) — deliberate simplicity for the workshop vertical.
        int updated = dsl.update(GIFT_CARD)
            .set(BALANCE, bal)
            .where(ID.eq(idStr))
            .execute();

        if (updated == 0) {
            dsl.insertInto(GIFT_CARD)
                .columns(ID, BALANCE)
                .values(idStr, bal)
                .execute();
        }
    }

    @Override
    public Optional<GiftCard> findById(GiftCardId id) {
        String idStr = id.value().toString();

        Result<Record2<String, BigDecimal>> rows = dsl
            .select(ID, BALANCE)
            .from(GIFT_CARD)
            .where(ID.eq(idStr))
            .fetch();

        if (rows.isEmpty()) {
            return Optional.empty();
        }

        Record2<String, BigDecimal> r = rows.getFirst();
        GiftCardId cardId = new GiftCardId(UUID.fromString(r.get(ID)));
        BigDecimal balance = r.get(BALANCE);

        return Optional.of(new GiftCard(cardId, new Money(balance)));
    }
}
