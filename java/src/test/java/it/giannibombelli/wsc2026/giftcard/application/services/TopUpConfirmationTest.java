package it.giannibombelli.wsc2026.giftcard.application.services;

import it.giannibombelli.wsc2026.common.domain.identity.EntityId;
import it.giannibombelli.wsc2026.common.domain.primitive.Description;
import it.giannibombelli.wsc2026.common.domain.primitive.Money;
import it.giannibombelli.wsc2026.giftcard.application.usecases.TopUpConfirming;
import it.giannibombelli.wsc2026.giftcard.domain.giftcard.GiftCard;
import it.giannibombelli.wsc2026.giftcard.domain.giftcard.GiftCardId;
import it.giannibombelli.wsc2026.giftcard.domain.policies.ConfirmTopUpPolicy;
import it.giannibombelli.wsc2026.giftcard.domain.ports.GiftCardRepository;
import it.giannibombelli.wsc2026.giftcard.infrastructure.SqliteGiftCardRepository;
import it.giannibombelli.wsc2026.payment.domain.events.PaymentResultEvents;
import it.giannibombelli.wsc2026.payment.domain.payment.Payment;
import it.giannibombelli.wsc2026.testsupport.AggregateFactory;
import it.giannibombelli.wsc2026.testsupport.DatabaseSetup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.util.Optional;

import static it.giannibombelli.wsc2026.testsupport.AggregateFactory.createPayment;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TopUpConfirmationTest {

    private GiftCardRepository repository;
    private TopUpConfirmation paymentService;

    @BeforeEach
    void setUp() {
        final DataSource dataSource = DatabaseSetup.initializeInMemoryDb("giftcard");
        repository = new SqliteGiftCardRepository(dataSource);

        ConfirmTopUpPolicy policy = new ConfirmTopUpPolicy();
        TopUpConfirming useCase = new TopUpConfirming(repository);
        paymentService = new TopUpConfirmation(policy, useCase);
    }

    @Nested
    class Construction {
        @Test
        void rejectNullParameters() {
            ConfirmTopUpPolicy policy = new ConfirmTopUpPolicy();
            TopUpConfirming useCase = new TopUpConfirming(repository);

            assertThatThrownBy(() -> new TopUpConfirmation(null, useCase))
                .isInstanceOf(NullPointerException.class);

            assertThatThrownBy(() -> new TopUpConfirmation(policy, null))
                .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    class PaymentResultsHandling {
        @Test
        void onAccepted_shouldUpdateBalance() {
            GiftCard issued = AggregateFactory.getSavedGiftCard(repository);
            Money amount = new Money(new BigDecimal("37.25"));
            Payment payment = createPayment(issued.id().value().toString(), amount);

            paymentService.handlePaymentResults(
                new PaymentResultEvents.PaymentAccepted(payment.id(), payment.clientReference(), amount)
            );

            Optional<GiftCard> updated = repository.findById(issued.id());
            assertThat(updated).isPresent();
            assertThat(updated.get().balance()).isEqualTo(amount);
        }

        @Test
        void onRejected_shouldDoNothing() {
            GiftCard issued = AggregateFactory.getSavedGiftCard(repository);
            Payment payment = createPayment();

            paymentService.handlePaymentResults(
                new PaymentResultEvents.PaymentRejected(payment.id(), payment.clientReference(), payment.amount(), new Description("declined"))
            );

            GiftCard persisted = repository.findById(issued.id()).orElseThrow();
            assertThat(persisted.balance()).isEqualTo(Money.zero());
        }

        @Test
        void onExpired_shouldDoNothing() {
            GiftCard issued = AggregateFactory.getSavedGiftCard(repository);
            Payment payment = createPayment();

            paymentService.handlePaymentResults(
                new PaymentResultEvents.PaymentExpired(payment.id(), payment.clientReference(), payment.amount())
            );

            GiftCard persisted = repository.findById(issued.id()).orElseThrow();
            assertThat(persisted.balance()).isEqualTo(Money.zero());
        }

        @Test
        void shouldFailIfCardDoesNotExist() {
            GiftCardId nonExisting = EntityId.generate(GiftCardId::new);
            Money amount = new Money(new BigDecimal("10.00"));
            Payment payment = createPayment(nonExisting.value().toString(), amount);

            assertThatThrownBy(() -> paymentService.handlePaymentResults(
                new PaymentResultEvents.PaymentAccepted(payment.id(), payment.clientReference(), amount)
            ))
                .isInstanceOf(IllegalStateException.class);
        }
    }
}
