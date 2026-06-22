package it.giannibombelli.wsc2026.giftcard.application.integration.payment.handlers;

import it.giannibombelli.wsc2026.common.domain.identity.EntityId;
import it.giannibombelli.wsc2026.common.domain.primitive.Money;
import it.giannibombelli.wsc2026.giftcard.application.integration.payment.adapter.PaymentResult;
import it.giannibombelli.wsc2026.giftcard.application.usecases.TopUpConfirming;
import it.giannibombelli.wsc2026.giftcard.domain.giftcard.GiftCard;
import it.giannibombelli.wsc2026.giftcard.domain.giftcard.GiftCardId;
import it.giannibombelli.wsc2026.giftcard.domain.ports.GiftCardRepository;
import it.giannibombelli.wsc2026.giftcard.infrastructure.SqliteGiftCardRepository;
import it.giannibombelli.wsc2026.payment.integration.PaymentResultIntegrationEvent;
import it.giannibombelli.wsc2026.testsupport.AggregateFactory;
import it.giannibombelli.wsc2026.testsupport.DatabaseSetup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConfirmTopUpFromPaymentTest {

    private GiftCardRepository repository;
    private ConfirmTopUpFromPayment confirmTopUpFromPayment;

    @BeforeEach
    void setUp() {
        final DataSource dataSource = DatabaseSetup.initializeInMemoryDb("giftcard");
        repository = new SqliteGiftCardRepository(dataSource);

        PaymentResult paymentResult = new PaymentResult();
        TopUpConfirming useCase = new TopUpConfirming(repository);
        confirmTopUpFromPayment = new ConfirmTopUpFromPayment(paymentResult, useCase);
    }

    @Nested
    class Construction {
        @Test
        void rejectNullParameters() {
            PaymentResult paymentResult = new PaymentResult();
            TopUpConfirming useCase = new TopUpConfirming(repository);

            assertThatThrownBy(() -> new ConfirmTopUpFromPayment(null, useCase))
                .isInstanceOf(NullPointerException.class);

            assertThatThrownBy(() -> new ConfirmTopUpFromPayment(paymentResult, null))
                .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    class PaymentResultsHandling {
        @Test
        void onAccepted_shouldUpdateBalance() {
            GiftCard issued = AggregateFactory.getSavedGiftCard(repository);
            Money amount = new Money(new BigDecimal("37.25"));

            confirmTopUpFromPayment.handle(
                new PaymentResultIntegrationEvent.PaymentAcceptedIntegrationEvent(issued.id().value().toString(), amount)
            );

            Optional<GiftCard> updated = repository.findById(issued.id());
            assertThat(updated).isPresent();
            assertThat(updated.get().balance()).isEqualTo(amount);
        }

        @Test
        void onRejected_shouldDoNothing() {
            GiftCard issued = AggregateFactory.getSavedGiftCard(repository);

            confirmTopUpFromPayment.handle(
                new PaymentResultIntegrationEvent.PaymentRejectedIntegrationEvent(
                    UUID.randomUUID().toString(), new Money(new BigDecimal("50.00")), "declined")
            );

            GiftCard persisted = repository.findById(issued.id()).orElseThrow();
            assertThat(persisted.balance()).isEqualTo(Money.zero());
        }

        @Test
        void onExpired_shouldDoNothing() {
            GiftCard issued = AggregateFactory.getSavedGiftCard(repository);

            confirmTopUpFromPayment.handle(
                new PaymentResultIntegrationEvent.PaymentExpiredIntegrationEvent(
                    UUID.randomUUID().toString(), new Money(new BigDecimal("50.00")))
            );

            GiftCard persisted = repository.findById(issued.id()).orElseThrow();
            assertThat(persisted.balance()).isEqualTo(Money.zero());
        }

        @Test
        void shouldFailIfCardDoesNotExist() {
            GiftCardId nonExisting = EntityId.generate(GiftCardId::new);
            Money amount = new Money(new BigDecimal("10.00"));

            assertThatThrownBy(() -> confirmTopUpFromPayment.handle(
                new PaymentResultIntegrationEvent.PaymentAcceptedIntegrationEvent(nonExisting.value().toString(), amount)
            ))
                .isInstanceOf(IllegalStateException.class);
        }
    }
}
