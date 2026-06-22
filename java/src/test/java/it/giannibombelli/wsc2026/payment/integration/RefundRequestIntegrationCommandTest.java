package it.giannibombelli.wsc2026.payment.integration;

import it.giannibombelli.wsc2026.common.domain.primitive.Money;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RefundRequestIntegrationCommandTest {

    @Test
    void create_withValidValues_returnsCommand() {
        String clientReference = UUID.randomUUID().toString();
        Money amount = new Money(new BigDecimal("50.00"));

        RefundRequestIntegrationCommand command = new RefundRequestIntegrationCommand(clientReference, amount);

        assertThat(command.clientReference()).isEqualTo(clientReference);
        assertThat(command.amount()).isEqualTo(amount);
    }

    @Test
    void create_withNullClientReference_throwsException() {
        Money amount = new Money(new BigDecimal("50.00"));

        assertThatThrownBy(() -> new RefundRequestIntegrationCommand(null, amount))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void create_withNullAmount_throwsException() {
        String clientReference = UUID.randomUUID().toString();

        assertThatThrownBy(() -> new RefundRequestIntegrationCommand(clientReference, null))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
