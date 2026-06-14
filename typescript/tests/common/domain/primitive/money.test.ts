import { describe, expect, it } from 'vitest';
import { Money } from '@/common/domain/primitive/money.js';

describe('Money', () => {
  it('dovrebbe creare un valore valido', () => {
    const money = new Money(10.50);
    expect(money.value).toBe(10.50);
  });

  it('dovrebbe creare uno zero', () => {
    expect(Money.zero().value).toBe(0);
  });

  it('dovrebbe normalizzare la scala', () => {
    const money = new Money(10.5);
    expect(money.value).toBe(10.5);
  });

  it('dovrebbe rifiutare un valore nullo', () => {
    expect(() => new Money(null as unknown as number)).toThrow('value must be defined');
  });

  it('dovrebbe rifiutare un valore negativo', () => {
    expect(() => new Money(-1.00)).toThrow('value must not be negative');
  });

  it('dovrebbe rifiutare più di due decimali', () => {
    expect(() => new Money(1.001)).toThrow('value must have at most 2 decimal places');
  });

  it('dovrebbe sommare', () => {
    const a = new Money(10.00);
    const b = new Money(5.50);
    expect(a.plus(b).value).toBe(15.50);
  });

  it('dovrebbe sottrarre', () => {
    const a = new Money(10.00);
    const b = new Money(3.25);
    expect(a.minus(b).value).toBe(6.75);
  });

  it('dovrebbe confrontare', () => {
    const a = new Money(10.00);
    const b = new Money(20.00);
    expect(a.isLessThan(b)).toBe(true);
    expect(b.isLessThan(a)).toBe(false);
  });

  it('dovrebbe controllare il segno', () => {
    expect(new Money(10.00).isPositive()).toBe(true);
    expect(Money.zero().isPositive()).toBe(false);
    expect(Money.zero().isZero()).toBe(true);
  });
});
