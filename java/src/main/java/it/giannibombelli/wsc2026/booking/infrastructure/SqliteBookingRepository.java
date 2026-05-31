package it.giannibombelli.wsc2026.booking.infrastructure;

import it.giannibombelli.wsc2026.booking.domain.booking.Booking;
import it.giannibombelli.wsc2026.booking.domain.booking.BookingId;
import it.giannibombelli.wsc2026.booking.domain.booking.BookingStatus;
import it.giannibombelli.wsc2026.booking.domain.ports.BookingRepository;
import it.giannibombelli.wsc2026.common.domain.primitive.Description;
import it.giannibombelli.wsc2026.giftcard.domain.giftcard.GiftCardId;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;

import javax.sql.DataSource;
import java.util.Optional;
import java.util.UUID;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.table;



public final class SqliteBookingRepository implements BookingRepository {
    private static final org.jooq.Table<?> BOOKING = table("booking");
    private static final org.jooq.Field<String> ID = field("id", String.class);
    private static final org.jooq.Field<String> DESCRIPTION = field("description", String.class);
    private static final org.jooq.Field<String> GIFT_CARD_ID = field("gift_card_id", String.class);
    private static final org.jooq.Field<String> STATUS = field("status", String.class);

    private final DSLContext dsl;

    public SqliteBookingRepository(DataSource dataSource) {
        this.dsl = DSL.using(dataSource, SQLDialect.SQLITE);
    }

    @Override
    public void save(Booking booking) {
        String idStr = booking.id().value().toString();

        String statusStr = booking.status().name();

        int updated = dsl.update(BOOKING)
            .set(ID, idStr)
            .set(DESCRIPTION, booking.description().value())
            .set(GIFT_CARD_ID, booking.giftCardId().value().toString())
            .set(STATUS, statusStr)
            .where(ID.eq(idStr))
            .execute();

        if (updated == 0) {
            dsl.insertInto(BOOKING)
                .columns(ID, DESCRIPTION, GIFT_CARD_ID, STATUS)
                .values(idStr, booking.description().value(), booking.giftCardId().value().toString(), statusStr)
                .execute();
        }
    }

    @Override
    public Optional<Booking> findById(BookingId id) {
        String idStr = id.value().toString();

        var rows = dsl
            .select(ID, DESCRIPTION, GIFT_CARD_ID, STATUS)
            .from(BOOKING)
            .where(ID.eq(idStr))
            .fetch();

        if (rows.isEmpty()) {
            return Optional.empty();
        }

        Record r = rows.getFirst();
        BookingId bookingId = new BookingId(UUID.fromString(r.get(ID)));
        Description description = new Description(r.get(DESCRIPTION));
        GiftCardId giftCardId = new GiftCardId(UUID.fromString(r.get(GIFT_CARD_ID)));
        BookingStatus status = BookingStatus.valueOf(r.get(STATUS));
        return Optional.of(new Booking(bookingId, description, giftCardId, status));
    }
}
