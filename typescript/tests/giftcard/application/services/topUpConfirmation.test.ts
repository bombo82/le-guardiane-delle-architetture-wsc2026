import { beforeAll, describe, expect, it } from 'vitest';
import { generateId } from '@/common/domain/identity/entityId.js';
import { ClientReference } from '@/common/domain/primitive/clientReference.js';
import { Description } from '@/common/domain/primitive/description.js';
import { Money } from '@/common/domain/primitive/money.js';
import { Timestamp } from '@/common/domain/primitive/timestamp.js';
import { Payment } from '@/payment/domain/payment/payment.js';
import { PaymentId } from '@/payment/domain/payment/paymentId.js';
import {
  paymentAccepted,
  paymentRejected,
  paymentExpired,
} from '@/payment/domain/events/paymentResultEvents.js';
import { TopUpConfirmation } from '@/giftcard/application/services/topUpConfirmation.js';
import { TopUpConfirming } from '@/giftcard/application/usecases/topUpConfirming.js';
import { ConfirmTopUpPolicy } from '@/giftcard/domain/policies/confirmTopUpPolicy.js';
import { GiftCardId } from '@/giftcard/domain/giftcard/giftCardId.js';
import { SqliteGiftCardRepository } from '@/giftcard/infrastructure/sqliteGiftCardRepository.js';
import { Uuid } from '@/common/domain/primitive/uuid.js';
import { DatabaseSetup } from '../../../testsupport/databaseSetup.js';
import { GiftCardAggregateFactory } from '../../../testsupport/giftcard/aggregateFactory.js';

describe('TopUpConfirmation', () => {
  let repository: SqliteGiftCardRepository;
  let service: TopUpConfirmation;

  beforeAll(() => {
    const database = DatabaseSetup.initializeInMemoryDb('giftcard');
    repository = new SqliteGiftCardRepository(database);
    const policy = new ConfirmTopUpPolicy();
    const useCase = new TopUpConfirming(repository);
    service = new TopUpConfirmation(policy, useCase);
  });

  function createPayment(clientReference: string, amount: Money): Payment {
    return Payment.request(
      generateId((value) => new PaymentId(value)),
      new ClientReference(Uuid.fromString(clientReference)),
      amount,
      Timestamp.now()
    );
  }

  describe('construction', () => {
    it('rejects null parameters', () => {
      const policy = new ConfirmTopUpPolicy();
      const useCase = new TopUpConfirming(repository);

      expect(() => new TopUpConfirmation(null as unknown as ConfirmTopUpPolicy, useCase)).toThrow();
      expect(() => new TopUpConfirmation(policy, null as unknown as TopUpConfirming)).toThrow();
    });
  });

  describe('payment results handling', () => {
    it('on accepted should update balance', () => {
      const issued = GiftCardAggregateFactory.getSavedGiftCard(repository);
      const amount = new Money(37.25);
      const payment = createPayment(issued.id().value.value, amount);
      const event = paymentAccepted(
        payment.id(),
        payment.clientReference(),
        amount
      );

      service.handlePaymentResults(event);

      const updated = repository.findById(issued.id());
      expect(updated).not.toBeNull();
      expect(updated!.balance()).toEqual(amount);
    });

    it('on rejected should do nothing', () => {
      const issued = GiftCardAggregateFactory.getSavedGiftCard(repository);
      const payment = createPayment(crypto.randomUUID(), new Money(50));
      const event = paymentRejected(
        payment.id(),
        payment.clientReference(),
        payment.amount(),
        new Description('declined')
      );

      service.handlePaymentResults(event);

      const persisted = repository.findById(issued.id());
      expect(persisted).not.toBeNull();
      expect(persisted!.balance()).toEqual(Money.zero());
    });

    it('on expired should do nothing', () => {
      const issued = GiftCardAggregateFactory.getSavedGiftCard(repository);
      const payment = createPayment(crypto.randomUUID(), new Money(50));
      const event = paymentExpired(
        payment.id(),
        payment.clientReference(),
        payment.amount()
      );

      service.handlePaymentResults(event);

      const persisted = repository.findById(issued.id());
      expect(persisted).not.toBeNull();
      expect(persisted!.balance()).toEqual(Money.zero());
    });

    it('should fail if card does not exist', () => {
      const nonExisting = generateId((value) => new GiftCardId(value));
      const amount = new Money(10);
      const payment = createPayment(nonExisting.value.value, amount);
      const event = paymentAccepted(
        payment.id(),
        payment.clientReference(),
        amount
      );

      expect(() => service.handlePaymentResults(event)).toThrow();
    });
  });
});
