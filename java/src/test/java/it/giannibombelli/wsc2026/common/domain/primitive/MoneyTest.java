package it.giannibombelli.wsc2026.common.domain.primitive;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MoneyTest {

    @Test
    void shouldCreateWithValidValue() {
        Money money = new Money(new BigDecimal("10.50"));
        assertThat(money.value()).isEqualTo(new BigDecimal("10.50"));
    }

    @Test
    void shouldCreateWithZero() {
        assertThat(Money.zero().value()).isEqualTo(new BigDecimal("0.00"));
    }

    @Test
    void shouldNormalizeScale() {
        Money money = new Money(new BigDecimal("10.5"));
        assertThat(money.value()).isEqualTo(new BigDecimal("10.50"));
    }

    @Test
    void shouldRejectNull() {
        assertThatThrownBy(() -> new Money(null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldRejectNegative() {
        assertThatThrownBy(() -> new Money(new BigDecimal("-1.00")))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldRejectMoreThanTwoDecimalPlaces() {
        assertThatThrownBy(() -> new Money(new BigDecimal("1.001")))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldAdd() {
        Money a = new Money(new BigDecimal("10.00"));
        Money b = new Money(new BigDecimal("5.50"));
        assertThat(a.plus(b)).isEqualTo(new Money(new BigDecimal("15.50")));
    }

    @Test
    void shouldSubtract() {
        Money a = new Money(new BigDecimal("10.00"));
        Money b = new Money(new BigDecimal("3.25"));
        assertThat(a.minus(b)).isEqualTo(new Money(new BigDecimal("6.75")));
    }

    @Test
    void shouldCompare() {
        Money a = new Money(new BigDecimal("10.00"));
        Money b = new Money(new BigDecimal("20.00"));
        assertThat(a.isLessThan(b)).isTrue();
        assertThat(b.isLessThan(a)).isFalse();
    }

    @Test
    void shouldCheckSign() {
        assertThat(new Money(new BigDecimal("10.00")).isPositive()).isTrue();
        assertThat(Money.zero().isPositive()).isFalse();
        assertThat(Money.zero().isZero()).isTrue();
    }
}
