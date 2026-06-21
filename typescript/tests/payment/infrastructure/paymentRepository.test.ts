import { beforeAll, describe, expect, it } from 'vitest';
import { generateId } from '@/common/domain/identity/entityId.js';
import { ClientReference } from '@/common/domain/primitive/clientReference.js';
import { Money } from '@/common/domain/primitive/money.js';
import { Timestamp } from '@/common/domain/primitive/timestamp.js';
import { Payment } from '@/payment/domain/payment/payment.js';
import { PaymentId } from '@/payment/domain/payment/paymentId.js';
import { PaymentStatus } from '@/payment/domain/payment/paymentStatus.js';
import { SqlitePaymentRepository } from '@/payment/infrastructure/sqlitePaymentRepository.js';
import { Uuid } from '@/common/domain/primitive/uuid.js';
import { DatabaseSetup } from '../../testsupport/databaseSetup.js';

describe('PaymentRepository', () => {
  let repository: SqlitePaymentRepository;

  beforeAll(() => {
    const database = DatabaseSetup.initializeFileDb('payment', 'PaymentRepositoryTest');
    repository = new SqlitePaymentRepository(database);
  });

  describe('save', () => {
    it('should persist new payment', () => {
      const paymentId = generateId((value) => new PaymentId(value));
      const clientReference = crypto.randomUUID();
      const amount = new Money(50);
      const requestedAt = new Timestamp(new Date('2026-06-07T10:00:00.000Z'));
      const original = Payment.request(paymentId, new ClientReference(Uuid.fromString(clientReference)), amount, requestedAt);

      repository.save(original);
      const reloaded = repository.findById(paymentId);

      expect(reloaded).not.toBeNull();
      expect(reloaded!.id()).toEqual(paymentId);
      expect(reloaded!.clientReference().toString()).toEqual(clientReference);
      expect(reloaded!.amount()).toEqual(amount);
      expect(reloaded!.status()).toEqual(PaymentStatus.REQUESTED);
      expect(reloaded!.requestedAt().value).toEqual(requestedAt.value);
    });
  });

  describe('findById', () => {
    it('should return empty when not found', () => {
      const nonExistentId = generateId((value) => new PaymentId(value));

      const reloaded = repository.findById(nonExistentId);

      expect(reloaded).toBeNull();
    });
  });

  describe('findByClientReference', () => {
    it('should return payment when client reference matches', () => {
      const paymentId = generateId((value) => new PaymentId(value));
      const clientReference = crypto.randomUUID();
      const amount = new Money(25);
      const payment = Payment.request(
        paymentId,
        new ClientReference(Uuid.fromString(clientReference)),
        amount,
        Timestamp.now()
      );
      repository.save(payment);

      const found = repository.findByClientReference(new ClientReference(Uuid.fromString(clientReference)));

      expect(found).not.toBeNull();
      expect(found!.id()).toEqual(paymentId);
    });

    it('should return empty when client reference does not match', () => {
      const found = repository.findByClientReference(new ClientReference(Uuid.fromString('00000000-0000-0000-0000-000000000001')));

      expect(found).toBeNull();
    });
  });

  describe('findAllRequestedAndProcessingBefore', () => {
    it('should return payments requested before threshold', () => {
      const paymentId = generateId((value) => new PaymentId(value));
      const requestedAt = new Timestamp(new Date('2026-06-07T10:00:00.000Z'));
      const payment = Payment.request(
        paymentId,
        new ClientReference(Uuid.fromString(crypto.randomUUID())),
        new Money(10),
        requestedAt
      );
      repository.save(payment);

      const found = repository.findAllRequestedAndProcessingBefore(
        new Timestamp(new Date('2026-06-08T10:00:00.000Z'))
      );

      expect(found.map((p) => p.id().value.value)).toContain(paymentId.value.value);
    });

    it('should not return payments requested after threshold', () => {
      const paymentId = generateId((value) => new PaymentId(value));
      const requestedAt = new Timestamp(new Date('2026-06-07T10:00:00.000Z'));
      const payment = Payment.request(
        paymentId,
        new ClientReference(Uuid.fromString(crypto.randomUUID())),
        new Money(10),
        requestedAt
      );
      repository.save(payment);

      const found = repository.findAllRequestedAndProcessingBefore(
        new Timestamp(new Date('2026-06-06T10:00:00.000Z'))
      );

      expect(found.map((p) => p.id().value)).not.toContain(paymentId.value);
    });
  });
});
