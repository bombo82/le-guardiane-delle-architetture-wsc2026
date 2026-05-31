package it.giannibombelli.wsc2026.payment.infrastructure;

import it.giannibombelli.wsc2026.common.utils.Require;

import it.giannibombelli.wsc2026.common.domain.primitive.ClientReference;
import it.giannibombelli.wsc2026.common.domain.primitive.Money;
import it.giannibombelli.wsc2026.common.domain.primitive.Timestamp;
import it.giannibombelli.wsc2026.payment.domain.payment.*;
import it.giannibombelli.wsc2026.payment.domain.ports.PaymentRepository;
import org.jooq.DSLContext;
import org.jooq.Record5;
import org.jooq.Result;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.table;

public final class SqlitePaymentRepository implements PaymentRepository {
    private static final org.jooq.Table<?> PAYMENT = table("payment");
    private static final org.jooq.Field<String> ID = field("id", String.class);
    private static final org.jooq.Field<String> CLIENT_REFERENCE = field("client_reference", String.class);
    private static final org.jooq.Field<BigDecimal> AMOUNT = field("amount", BigDecimal.class);
    private static final org.jooq.Field<String> STATUS = field("status", String.class);
    private static final org.jooq.Field<Instant> REQUESTED_AT = field("requested_at", Instant.class);

    private static final org.jooq.Table<?> TRANSACTION = table("payment_transaction");
    private static final org.jooq.Field<String> TX_PAYMENT_ID = field("payment_id", String.class);
    private static final org.jooq.Field<String> TX_TRANSACTION_ID = field("transaction_id", String.class);
    private static final org.jooq.Field<String> TX_PROVIDER = field("provider", String.class);
    private static final org.jooq.Field<String> TX_PROVIDER_REFERENCE = field("provider_reference", String.class);
    private static final org.jooq.Field<BigDecimal> TX_AMOUNT = field("amount", BigDecimal.class);
    private static final org.jooq.Field<Instant> TX_STARTED_AT = field("started_at", Instant.class);
    private static final org.jooq.Field<Instant> TX_PROVIDER_COMPLETED_AT = field("provider_completed_at", Instant.class);
    private static final org.jooq.Field<String> TX_STATUS = field("status", String.class);

    private final DSLContext dsl;

    public SqlitePaymentRepository(DataSource dataSource) {
        this.dsl = DSL.using(dataSource, SQLDialect.SQLITE);
    }

    @Override
    public void save(Payment payment) {
        String paymentId = payment.id().value().toString();

        int updated = dsl.update(PAYMENT)
            .set(CLIENT_REFERENCE, payment.clientReference().value())
            .set(AMOUNT, payment.amount().value())
            .set(STATUS, payment.status().name())
            .set(REQUESTED_AT, payment.requestedAt().value())
            .where(ID.eq(paymentId))
            .execute();

        if (updated == 0) {
            dsl.insertInto(PAYMENT)
                .columns(ID, CLIENT_REFERENCE, AMOUNT, STATUS, REQUESTED_AT)
                .values(paymentId, payment.clientReference().value(), payment.amount().value(),
                    payment.status().name(), payment.requestedAt().value())
                .execute();
        }

        dsl.deleteFrom(TRANSACTION)
            .where(TX_PAYMENT_ID.eq(paymentId))
            .execute();

        for (Transaction transaction : payment.transactions()) {
            insertTransaction(paymentId, transaction);
        }
    }

    @Override
    public Optional<Payment> findById(PaymentId id) {
        String idStr = id.value().toString();

        Result<Record5<String, String, BigDecimal, String, Instant>> rows = dsl
            .select(ID, CLIENT_REFERENCE, AMOUNT, STATUS, REQUESTED_AT)
            .from(PAYMENT)
            .where(ID.eq(idStr))
            .fetch();

        if (rows.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(toDomainPayment(rows.getFirst(), loadTransactions(idStr)));
    }

    @Override
    public Optional<Payment> findByClientReference(ClientReference clientReference) {
        Require.requireArgument(clientReference, "clientReference");

        Result<Record5<String, String, BigDecimal, String, Instant>> rows = dsl
            .select(ID, CLIENT_REFERENCE, AMOUNT, STATUS, REQUESTED_AT)
            .from(PAYMENT)
            .where(CLIENT_REFERENCE.eq(clientReference.value()))
            .fetch();

        if (rows.isEmpty()) {
            return Optional.empty();
        }

        Record5<String, String, BigDecimal, String, Instant> r = rows.getFirst();
        return Optional.of(toDomainPayment(r, loadTransactions(r.get(ID))));
    }

    @Override
    public List<Payment> findAllRequestedAndProcessingBefore(Timestamp threshold) {
        Result<Record5<String, String, BigDecimal, String, Instant>> rows = dsl
            .select(ID, CLIENT_REFERENCE, AMOUNT, STATUS, REQUESTED_AT)
            .from(PAYMENT)
            .where(STATUS.in(PaymentStatus.REQUESTED.name(), PaymentStatus.PROCESSING.name()))
            .and(REQUESTED_AT.le(threshold.value()))
            .fetch();

        List<Payment> payments = new ArrayList<>();
        for (Record5<String, String, BigDecimal, String, Instant> record : rows) {
            payments.add(toDomainPayment(record, loadTransactions(record.get(ID))));
        }

        return payments;
    }

    private List<Transaction> loadTransactions(String paymentIdStr) {
        var txRows = dsl.select(TX_TRANSACTION_ID, TX_PROVIDER, TX_PROVIDER_REFERENCE, TX_AMOUNT, TX_STARTED_AT, TX_PROVIDER_COMPLETED_AT, TX_STATUS)
            .from(TRANSACTION)
            .where(TX_PAYMENT_ID.eq(paymentIdStr))
            .fetch();

        List<Transaction> transactions = new ArrayList<>();
        for (var record : txRows) {
            transactions.add(toDomainTransaction(record));
        }
        return transactions;
    }

    private Payment toDomainPayment(Record5<String, String, BigDecimal, String, Instant> record, List<Transaction> transactions) {
        PaymentId paymentId = new PaymentId(UUID.fromString(record.get(ID)));
        ClientReference clientReference = new ClientReference(record.get(CLIENT_REFERENCE));
        Money amount = new Money(record.get(AMOUNT));
        PaymentStatus status = PaymentStatus.valueOf(record.get(STATUS));
        Instant requestedAt = record.get(REQUESTED_AT) == null ? Instant.EPOCH : record.get(REQUESTED_AT);

        return new Payment(paymentId, clientReference, amount, status, new Timestamp(requestedAt), transactions);
    }

    private Transaction toDomainTransaction(org.jooq.Record record) {
        TransactionId id = new TransactionId(UUID.fromString(record.get(TX_TRANSACTION_ID)));
        Provider provider = record.get(TX_PROVIDER) == null ? null : Provider.fromLabel(record.get(TX_PROVIDER));
        ProviderReference providerReference = record.get(TX_PROVIDER_REFERENCE) == null ? null : new ProviderReference(UUID.fromString(record.get(TX_PROVIDER_REFERENCE)));
        Money amount = new Money(record.get(TX_AMOUNT));
        TransactionStatus status = TransactionStatus.valueOf(record.get(TX_STATUS));
        Timestamp startedAt = record.get(TX_STARTED_AT) == null ? new Timestamp(Instant.EPOCH) : new Timestamp(record.get(TX_STARTED_AT));
        Timestamp completedAt = record.get(TX_PROVIDER_COMPLETED_AT) == null ? null : new Timestamp(record.get(TX_PROVIDER_COMPLETED_AT));

        return new Transaction(id, provider, providerReference, amount, status, startedAt, completedAt);
    }

    private void insertTransaction(String paymentId, Transaction transaction) {
        dsl.insertInto(TRANSACTION)
            .columns(TX_PAYMENT_ID, TX_TRANSACTION_ID, TX_PROVIDER, TX_PROVIDER_REFERENCE, TX_AMOUNT, TX_STARTED_AT, TX_PROVIDER_COMPLETED_AT, TX_STATUS)
            .values(
                paymentId,
                transaction.id().value().toString(),
                transaction.provider().label(),
                toNullableString(transaction.providerReference()),
                transaction.amount().value(),
                transaction.startedAt().value(),
                toNullableInstant(transaction.completedAt()),
                transaction.status().name()
            )
            .execute();
    }

    private String toNullableString(Provider provider) {
        return provider == null ? null : provider.label();
    }

    private String toNullableString(ProviderReference providerReference) {
        return providerReference == null ? null : providerReference.value().toString();
    }

    private Instant toNullableInstant(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.value();
    }
}
