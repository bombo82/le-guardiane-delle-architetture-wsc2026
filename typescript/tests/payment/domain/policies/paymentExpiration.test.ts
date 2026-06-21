import { describe, expect, it } from 'vitest';
import { generateId } from '@/common/domain/identity/entityId.js';
import { ClientReference } from '@/common/domain/primitive/clientReference.js';
import { Money } from '@/common/domain/primitive/money.js';
import { Timestamp } from '@/common/domain/primitive/timestamp.js';
import { paymentDeadlineReached, type PaymentDeadlineReached } from '@/payment/domain/events/paymentDeadlineReached.js';
import { Payment } from '@/payment/domain/payment/payment.js';
import { PaymentId } from '@/payment/domain/payment/paymentId.js';
import { PaymentExpiration } from '@/payment/domain/policies/paymentExpiration.js';
import { Uuid } from '@/common/domain/primitive/uuid.js';

describe('PaymentExpiration', () => {
  const policy = new PaymentExpiration();

  it('isDeadlineReached returns true when deadline passed', () => {
    const requestedAt = new Date('2026-06-01T00:00:00.000Z');
    const now = new Timestamp(new Date(requestedAt.getTime() + 48 * 3600 * 1000 + 1));
    const payment = paymentRequestedAt(requestedAt);

    expect(policy.isDeadlineReached(payment, now)).toBe(true);
  });

  it('isDeadlineReached returns false when deadline not passed', () => {
    const requestedAt = new Date('2026-06-01T00:00:00.000Z');
    const now = new Timestamp(new Date(requestedAt.getTime() + 47 * 3600 * 1000));
    const payment = paymentRequestedAt(requestedAt);

    expect(policy.isDeadlineReached(payment, now)).toBe(false);
  });

  it('isDeadlineReached returns true exactly at deadline', () => {
    const requestedAt = new Date('2026-06-01T00:00:00.000Z');
    const now = new Timestamp(new Date(requestedAt.getTime() + 48 * 3600 * 1000));
    const payment = paymentRequestedAt(requestedAt);

    expect(policy.isDeadlineReached(payment, now)).toBe(true);
  });

  it('isDeadlineReached rejects null payment', () => {
    expect(() => policy.isDeadlineReached(null as unknown as Payment, Timestamp.now())).toThrow();
  });

  it('isDeadlineReached rejects null now', () => {
    const payment = paymentRequestedAt(new Date());

    expect(() => policy.isDeadlineReached(payment, null as unknown as Timestamp)).toThrow();
  });

  it('evaluate returns expire payment command', () => {
    const payment = paymentRequestedAt(new Date());
    const event = paymentDeadlineReached(payment.id());

    const cmd = policy.evaluate(event);

    expect(cmd.aggregateId).toEqual(payment.id());
  });

  it('evaluate rejects null event', () => {
    expect(() => policy.evaluate(null as unknown as PaymentDeadlineReached)).toThrow();
  });

  function paymentRequestedAt(requestedAt: Date): Payment {
    const paymentId = generateId((value) => new PaymentId(value));
    return Payment.request(
      paymentId,
      new ClientReference(Uuid.fromString(crypto.randomUUID())),
      new Money(50),
      new Timestamp(requestedAt)
    );
  }
});
