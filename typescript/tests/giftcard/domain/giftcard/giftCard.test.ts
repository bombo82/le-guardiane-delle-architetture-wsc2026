import { describe, expect, it } from 'vitest';
import { generateId } from '@/common/domain/identity/entityId.js';
import { Money } from '@/common/domain/primitive/money.js';
import { GiftCard } from '@/giftcard/domain/giftcard/giftCard.js';
import { GiftCardId } from '@/giftcard/domain/giftcard/giftCardId.js';
import type { GiftCardRedeemed, GiftCardNotRedeemed } from '@/giftcard/domain/events/giftCardRedeemEvents.js';
import { GiftCardAggregateFactory } from '../../../testsupport/giftcard/aggregateFactory.js';

describe('GiftCard', () => {
  describe('issuance', () => {
    it('should issue', () => {
      const id = generateId((value) => new GiftCardId(value));

      const giftCard = GiftCard.issue(id);

      expect(giftCard.id()).toEqual(id);
      expect(giftCard.balance()).toEqual(Money.zero());
    });

    it('should fail if id is null', () => {
      expect(() => GiftCard.issue(null as unknown as GiftCardId)).toThrow();
    });
  });

  describe('topUpRequest', () => {
    it('should fail if amount is null', () => {
      const giftCard = GiftCardAggregateFactory.createGiftCard();

      expect(() => giftCard.requestTopUp(null as unknown as Money)).toThrow();
    });

    it('should emit event', () => {
      const giftCard = GiftCardAggregateFactory.createGiftCard();
      const topUpAmount = new Money(25);

      const result = giftCard.requestTopUp(topUpAmount);

      expect(result.aggregateId).toEqual(giftCard.id());
      expect(result.requestedAmount).toEqual(topUpAmount);
    });
  });

  describe('topUpConfirmation', () => {
    it('should fail if amount is null', () => {
      const giftCard = GiftCardAggregateFactory.createGiftCard();

      expect(() => giftCard.confirmTopUp(null as unknown as Money)).toThrow();
    });

    it('should increase balance', () => {
      const giftCard = GiftCardAggregateFactory.createGiftCard(new Money(10));
      const topUpAmount = new Money(50);

      const result = giftCard.confirmTopUp(topUpAmount);

      expect(result.aggregateId).toEqual(giftCard.id());
      expect(giftCard.balance()).toEqual(new Money(60));
      expect(giftCard.id()).toEqual(giftCard.id());
    });
  });

  describe('redemption', () => {
    it('should fail if amount is null', () => {
      const giftCard = GiftCardAggregateFactory.createGiftCard(new Money(100));

      expect(() => giftCard.redeem(null as unknown as Money)).toThrow();
    });

    it('should succeed with sufficient balance', () => {
      const giftCard = GiftCardAggregateFactory.createGiftCard(new Money(100));
      const redeemAmount = new Money(37.5);

      const result = giftCard.redeem(redeemAmount);

      expect(result.kind).toBe('GiftCardRedeemed');
      const redeemed = result as GiftCardRedeemed;
      expect(redeemed.aggregateId).toEqual(giftCard.id());
      expect(redeemed.redeemedAmount).toEqual(redeemAmount);
      expect(giftCard.balance()).toEqual(new Money(62.5));
      expect(giftCard.id()).toEqual(giftCard.id());
    });

    it('should fail with insufficient balance', () => {
      const giftCard = GiftCardAggregateFactory.createGiftCard(new Money(20));
      const bigRedeem = new Money(50);

      const result = giftCard.redeem(bigRedeem);

      expect(result.kind).toBe('GiftCardNotRedeemed');
      const notRedeemed = result as GiftCardNotRedeemed;
      expect(notRedeemed.aggregateId).toEqual(giftCard.id());
      expect(notRedeemed.attemptedAmount).toEqual(bigRedeem);
      expect(notRedeemed.reason.value).toEqual('insufficient balance');
      expect(giftCard.balance()).toEqual(new Money(20));
    });
  });

  describe('refunding', () => {
    it('should fail if amount is null', () => {
      const giftCard = GiftCardAggregateFactory.createGiftCard(new Money(60));

      expect(() => giftCard.refund(null as unknown as Money)).toThrow();
    });

    it('should restore balance', () => {
      const giftCard = GiftCardAggregateFactory.createGiftCard(new Money(60));
      const refundAmount = new Money(25);

      const result = giftCard.refund(refundAmount);

      expect(result.aggregateId).toEqual(giftCard.id());
      expect(result.refundedAmount).toEqual(refundAmount);
      expect(giftCard.balance()).toEqual(new Money(85));
      expect(giftCard.id()).toEqual(giftCard.id());
    });
  });

  describe('crediting', () => {
    it('should fail if amount is null', () => {
      const giftCard = GiftCardAggregateFactory.createGiftCard(new Money(35));

      expect(() => giftCard.credit(null as unknown as Money)).toThrow();
    });

    it('should increase balance', () => {
      const giftCard = GiftCardAggregateFactory.createGiftCard(new Money(35));
      const creditAmount = new Money(10);

      const result = giftCard.credit(creditAmount);

      expect(result.aggregateId).toEqual(giftCard.id());
      expect(result.creditedAmount).toEqual(creditAmount);
      expect(giftCard.balance()).toEqual(new Money(45));
      expect(giftCard.id()).toEqual(giftCard.id());
    });
  });
});
