package it.giannibombelli.wsc2026.common.domain.primitive;

import it.giannibombelli.wsc2026.common.utils.Require;

import java.math.BigDecimal;
import java.math.RoundingMode;

public record Money(BigDecimal value) {
    public Money {
        Require.requireArgument(value, "value");
        try {
            value = value.setScale(2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException e) {
            throw new IllegalArgumentException("value must have at most 2 decimal places");
        }
        if (value.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("value must not be negative");
        }
    }

    public boolean isPositive() {
        return value.compareTo(BigDecimal.ZERO) > 0;
    }

    public boolean isZero() {
        return value.compareTo(BigDecimal.ZERO) == 0;
    }

    public boolean isLessThan(Money other) {
        return this.value.compareTo(other.value) < 0;
    }

    public boolean isGreaterThan(Money other) {
        return this.value.compareTo(other.value) > 0;
    }

    public Money plus(Money other) {
        return new Money(this.value.add(other.value));
    }

    public Money minus(Money other) {
        return new Money(this.value.subtract(other.value));
    }

    public static Money zero() {
        return new Money(BigDecimal.ZERO);
    }
}
