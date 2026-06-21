import { describe, expect, it } from 'vitest';
import { generateId } from '@/common/domain/identity/entityId.js';
import { ClientReference } from '@/common/domain/primitive/clientReference.js';
import { Description } from '@/common/domain/primitive/description.js';
import { Money } from '@/common/domain/primitive/money.js';
import { Timestamp } from '@/common/domain/primitive/timestamp.js';
import { Payment } from '@/payment/domain/payment/payment.js';
import { PaymentId } from '@/payment/domain/payment/paymentId.js';
import { PaymentStatus } from '@/payment/domain/payment/paymentStatus.js';
import { providerFromLabel } from '@/payment/domain/payment/provider.js';
import { ProviderReference } from '@/payment/domain/payment/providerReference.js';
import { TransactionId } from '@/payment/domain/payment/transactionId.js';
import { Uuid } from '@/common/domain/primitive/uuid.js';


const CLIENT_REFERENCE = 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa';
const TOTAL = new Money(100);
const PAYPAL = 'PayPal';
const KLARNA = 'Klarna';

describe('PaymentTransactionDecision', () => {
  it('single timely accept leads to payment accepted', () => {
    const requestedAt = new Timestamp(new Date('2026-06-07T10:00:00.000Z'));
    const providerCompletion = new Timestamp(new Date('2026-06-07T11:00:00.000Z'));

    const pid = new PaymentId(Uuid.generate());
    const p = Payment.request(pid, new ClientReference(Uuid.fromString(CLIENT_REFERENCE)), TOTAL, requestedAt);
    const txId = startTransaction(p, PAYPAL, TOTAL, requestedAt.plusSeconds(1));

    const evt = p.acceptTransaction(txId, providerCompletion);

    expect(evt!.kind).toBe('PaymentAccepted');
    expect(p.status()).toEqual(PaymentStatus.ACCEPTED);
  });

  it('any single reject rejects the entire payment', () => {
    const requestedAt = new Timestamp(new Date('2026-06-07T09:00:00.000Z'));

    const pid = new PaymentId(Uuid.generate());
    const p = Payment.request(pid, new ClientReference(Uuid.fromString(CLIENT_REFERENCE)), TOTAL, requestedAt);
    const txA = startTransaction(p, PAYPAL, new Money(40), requestedAt.plusSeconds(1));
    p.acceptTransaction(txA, requestedAt.plusSeconds(3600));

    const txB = startTransaction(p, KLARNA, new Money(60), requestedAt.plusSeconds(2));
    const evt = p.rejectTransaction(txB, new Description('declined'));

    expect(evt.kind).toBe('PaymentRejected');
    expect(p.status()).toEqual(PaymentStatus.REJECTED);
  });

  it('all accepted but insufficient coverage stays processing not accepted', () => {
    const requestedAt = new Timestamp(new Date('2026-06-07T10:00:00.000Z'));
    const txTime = new Timestamp(new Date('2026-06-07T10:30:00.000Z'));

    const pid = new PaymentId(Uuid.generate());
    const p = Payment.request(pid, new ClientReference(Uuid.fromString(CLIENT_REFERENCE)), TOTAL, requestedAt);
    const txId = startTransaction(p, PAYPAL, new Money(60), requestedAt.plusSeconds(1));

    p.acceptTransaction(txId, txTime);

    expect(p.status()).toEqual(PaymentStatus.PROCESSING);
  });

  it('payment accepted only when all tx accepted and all times within 48h window', () => {
    const requestedAt = new Timestamp(new Date('2026-06-07T08:00:00.000Z'));
    const tx1 = new Timestamp(new Date('2026-06-07T09:00:00.000Z'));
    const tx2 = new Timestamp(new Date('2026-06-08T06:59:59.000Z'));

    const pid = new PaymentId(Uuid.generate());
    const p = Payment.request(pid, new ClientReference(Uuid.fromString(CLIENT_REFERENCE)), TOTAL, requestedAt);
    const txA = startTransaction(p, PAYPAL, new Money(60), requestedAt.plusSeconds(1));
    p.acceptTransaction(txA, tx1);
    const txB = startTransaction(p, KLARNA, new Money(40), requestedAt.plusSeconds(2));
    const finalEvt = p.acceptTransaction(txB, tx2);

    expect(finalEvt!.kind).toBe('PaymentAccepted');
    expect(p.status()).toEqual(PaymentStatus.ACCEPTED);
  });

  it('provider time beyond 48h does not count as timely accept for final state', () => {
    const requestedAt = new Timestamp(new Date('2026-06-01T12:00:00.000Z'));
    const timely = new Timestamp(new Date('2026-06-01T13:00:00.000Z'));
    const late = new Timestamp(new Date('2026-06-06T00:00:00.000Z'));

    const pid = new PaymentId(Uuid.generate());
    const p = Payment.request(pid, new ClientReference(Uuid.fromString(CLIENT_REFERENCE)), TOTAL, requestedAt);
    const txA = startTransaction(p, PAYPAL, new Money(50), requestedAt.plusSeconds(1));
    p.acceptTransaction(txA, timely);
    const txB = startTransaction(p, KLARNA, new Money(50), requestedAt.plusSeconds(2));

    expect(() => p.acceptTransaction(txB, late)).toThrow();

    expect(p.status()).toEqual(PaymentStatus.PROCESSING);
  });

  function startTransaction(payment: Payment, provider: string, amount: Money, startedAt: Timestamp): TransactionId {
    const transactionId = generateId((value) => new TransactionId(value));
    payment.startTransaction(
      transactionId,
      providerFromLabel(provider),
      new ProviderReference(Uuid.generate()),
      amount,
      startedAt
    );
    return transactionId;
  }
});
