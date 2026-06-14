import { requireArgument } from '@/common/utils/requireArgument.js';
function hasMoreThanTwoDecimals(value: number): boolean {
  const str = value.toString();
  const decimalPointIndex = str.indexOf('.');
  if (decimalPointIndex === -1) {
    return false;
  }
  return str.length - decimalPointIndex - 1 > 2;
}

export class Money {
  readonly value: number;

  constructor(value: number) {
    requireArgument(value, 'value');

    if (value < 0) {
      throw new Error('value must not be negative');
    }

    if (hasMoreThanTwoDecimals(value)) {
      throw new Error('value must have at most 2 decimal places');
    }

    this.value = value;
  }

  isPositive(): boolean {
    return this.value > 0;
  }

  isZero(): boolean {
    return this.value === 0;
  }

  isLessThan(other: Money): boolean {
    return this.value < other.value;
  }

  isGreaterThan(other: Money): boolean {
    return this.value > other.value;
  }

  equals(other: Money): boolean {
    return this.value === other.value;
  }

  plus(other: Money): Money {
    return new Money(this.value + other.value);
  }

  minus(other: Money): Money {
    return new Money(this.value - other.value);
  }

  static zero(): Money {
    return new Money(0);
  }
}
