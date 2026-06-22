import { beforeAll, describe, expect, it } from 'vitest';
import { generateId } from '@/common/domain/identity/entityId.js';
import { Money } from '@/common/domain/primitive/money.js';
import {
  paymentAcceptedIntegrationEvent,
  paymentExpiredIntegrationEvent,
  paymentRejectedIntegrationEvent,
} from '@/payment/integration/paymentResultIntegrationEvent.js';
import { ConfirmTopUpFromPayment } from '@/giftcard/application/integration/payment/handlers/confirmTopUpFromPayment.js';
import { TopUpConfirming } from '@/giftcard/application/usecases/topUpConfirming.js';
import { PaymentResult } from '@/giftcard/application/integration/payment/adapter/paymentResult.js';
import { GiftCardId } from '@/giftcard/domain/giftcard/giftCardId.js';
import { SqliteGiftCardRepository } from '@/giftcard/infrastructure/sqliteGiftCardRepository.js';
import { DatabaseSetup } from '../../../../../testsupport/databaseSetup.js';
import { GiftCardAggregateFactory } from '../../../../../testsupport/giftcard/aggregateFactory.js';

describe('ConfirmTopUpFromPayment', () => {
  let repository: SqliteGiftCardRepository;
  let confirmTopUpFromPayment: ConfirmTopUpFromPayment;

  beforeAll(() => {
    const database = DatabaseSetup.initializeInMemoryDb('giftcard');
    repository = new SqliteGiftCardRepository(database);
    const paymentResult = new PaymentResult();
    const useCase = new TopUpConfirming(repository);
    confirmTopUpFromPayment = new ConfirmTopUpFromPayment(paymentResult, useCase);
  });

  describe('construction', () => {
    it('rejects null parameters', () => {
      const paymentResult = new PaymentResult();
      const useCase = new TopUpConfirming(repository);

      expect(() => new ConfirmTopUpFromPayment(null as unknown as PaymentResult, useCase)).toThrow();
      expect(() => new ConfirmTopUpFromPayment(paymentResult, null as unknown as TopUpConfirming)).toThrow();
    });
  });

  describe('payment results handling', () => {
    it('on accepted should update balance', () => {
      const issued = GiftCardAggregateFactory.getSavedGiftCard(repository);
      const amount = new Money(37.25);

      confirmTopUpFromPayment.handle(
        paymentAcceptedIntegrationEvent(issued.id().value.value, amount)
      );

      const updated = repository.findById(issued.id());
      expect(updated).not.toBeNull();
      expect(updated!.balance()).toEqual(amount);
    });

    it('on rejected should do nothing', () => {
      const issued = GiftCardAggregateFactory.getSavedGiftCard(repository);

      confirmTopUpFromPayment.handle(
        paymentRejectedIntegrationEvent(issued.id().value.value, new Money(50), 'declined')
      );

      const persisted = repository.findById(issued.id());
      expect(persisted).not.toBeNull();
      expect(persisted!.balance()).toEqual(Money.zero());
    });

    it('on expired should do nothing', () => {
      const issued = GiftCardAggregateFactory.getSavedGiftCard(repository);

      confirmTopUpFromPayment.handle(
        paymentExpiredIntegrationEvent(issued.id().value.value, new Money(50))
      );

      const persisted = repository.findById(issued.id());
      expect(persisted).not.toBeNull();
      expect(persisted!.balance()).toEqual(Money.zero());
    });

    it('should fail if card does not exist', () => {
      const nonExisting = generateId((value) => new GiftCardId(value));
      const amount = new Money(10);

      expect(() =>
        confirmTopUpFromPayment.handle(
          paymentAcceptedIntegrationEvent(nonExisting.value.value, amount)
        )
      ).toThrow();
    });
  });
});
