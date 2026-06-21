import { beforeEach, describe, expect, it } from 'vitest';
import { generateId } from '@/common/domain/identity/entityId.js';
import { ClientReference } from '@/common/domain/primitive/clientReference.js';
import { Money } from '@/common/domain/primitive/money.js';
import { Timestamp } from '@/common/domain/primitive/timestamp.js';
import { PaymentFinder } from '@/payment/application/query/paymentFinder.js';
import { PaymentDetails } from '@/payment/application/query/paymentDetails.js';
import { PaymentSummary } from '@/payment/application/query/paymentSummary.js';
import { Payment } from '@/payment/domain/payment/payment.js';
import { PaymentId } from '@/payment/domain/payment/paymentId.js';
import { PaymentStatus } from '@/payment/domain/payment/paymentStatus.js';
import { SqlitePaymentRepository } from '@/payment/infrastructure/sqlitePaymentRepository.js';
import { DatabaseSetup } from '../../../testsupport/databaseSetup.js';
import { Uuid } from '@/common/domain/primitive/uuid.js';

const REQUESTED_AT = new Timestamp(new Date('2026-06-07T10:00:00.000Z'));

describe('PaymentFinder', () => {
  let repository: SqlitePaymentRepository;
  let finder: PaymentFinder;

  beforeEach(() => {
    const database = DatabaseSetup.initializeInMemoryDb('payment');
    repository = new SqlitePaymentRepository(database);
    finder = new PaymentFinder(repository);
  });

  it('should find payment summary by id', () => {
    const paymentId = generateId((value) => new PaymentId(value));
    const clientReference = crypto.randomUUID();
    const amount = new Money(50);
    const payment = Payment.request(paymentId, new ClientReference(Uuid.fromString(clientReference)), amount, REQUESTED_AT);
    repository.save(payment);

    const result = finder.findSummaryById(paymentId);

    expect(result).not.toBeNull();
    expect(result!.id).toEqual(paymentId.value.value);
    expect(result!.clientReference.toString()).toEqual(clientReference);
    expect(result!.amount).toEqual(amount);
    expect(result!.status).toEqual(PaymentStatus.REQUESTED);
  });

  it('should find payment details by id', () => {
    const paymentId = generateId((value) => new PaymentId(value));
    const clientReference = crypto.randomUUID();
    const amount = new Money(50);
    const payment = Payment.request(paymentId, new ClientReference(Uuid.fromString(clientReference)), amount, REQUESTED_AT);
    repository.save(payment);

    const result = finder.findDetailsById(paymentId);

    expect(result).not.toBeNull();
    expect(result!.id).toEqual(paymentId.value.value);
    expect(result!.transactions).toHaveLength(0);
  });

  it('should return empty when payment not found', () => {
    const paymentId = generateId((value) => new PaymentId(value));

    const result = finder.findSummaryById(paymentId);

    expect(result).toBeNull();
  });

  it('should throw when id is null', () => {
    expect(() => finder.findSummaryById(null as unknown as PaymentId)).toThrow();
  });

  it('should find details type guard', () => {
    const paymentId = generateId((value) => new PaymentId(value));
    repository.save(Payment.request(paymentId, new ClientReference(Uuid.fromString(crypto.randomUUID())), new Money(10), REQUESTED_AT));

    const details: PaymentDetails | null = finder.findDetailsById(paymentId);
    const summary: PaymentSummary | null = finder.findSummaryById(paymentId);

    expect(details).not.toBeNull();
    expect(summary).not.toBeNull();
  });
});
