import { describe, expect, it } from 'vitest';
import { generateId } from '@/common/domain/identity/entityId.js';
import { Money } from '@/common/domain/primitive/money.js';
import { confirmTopUp } from '@/giftcard/application/commands/confirmTopUp.js';
import { creditGiftCard } from '@/giftcard/application/commands/creditGiftCard.js';
import { issueGiftCard } from '@/giftcard/application/commands/issueGiftCard.js';
import { redeemGiftCard } from '@/giftcard/application/commands/redeemGiftCard.js';
import { refundGiftCard } from '@/giftcard/application/commands/refundGiftCard.js';
import { requestGiftCardTopUp } from '@/giftcard/application/commands/requestGiftCardTopUp.js';
import { GiftCardId } from '@/giftcard/domain/giftcard/giftCardId.js';

describe('GiftCard commands', () => {
  describe('IssueGiftCard validation', () => {
    it('should validate', () => {
      expect(() => issueGiftCard(null as unknown as GiftCardId)).toThrow();
    });
  });

  describe('RequestGiftCardTopUp validation', () => {
    it('should validate', () => {
      const cardId = generateId((value) => new GiftCardId(value));

      expect(() => requestGiftCardTopUp(null as unknown as GiftCardId, new Money(10))).toThrow();
      expect(() => requestGiftCardTopUp(cardId, null as unknown as Money)).toThrow();
    });
  });

  describe('ConfirmTopUp validation', () => {
    it('should validate', () => {
      const cardId = generateId((value) => new GiftCardId(value));

      expect(() => confirmTopUp(null as unknown as GiftCardId, new Money(10))).toThrow();
      expect(() => confirmTopUp(cardId, null as unknown as Money)).toThrow();
    });
  });

  describe('RedeemGiftCard validation', () => {
    it('should validate', () => {
      const cardId = generateId((value) => new GiftCardId(value));

      expect(() => redeemGiftCard(null as unknown as GiftCardId, new Money(10))).toThrow();
      expect(() => redeemGiftCard(cardId, null as unknown as Money)).toThrow();
    });
  });

  describe('RefundGiftCard validation', () => {
    it('should validate', () => {
      const cardId = generateId((value) => new GiftCardId(value));

      expect(() => refundGiftCard(null as unknown as GiftCardId, new Money(10))).toThrow();
      expect(() => refundGiftCard(cardId, null as unknown as Money)).toThrow();
    });
  });

  describe('CreditGiftCard validation', () => {
    it('should validate', () => {
      const cardId = generateId((value) => new GiftCardId(value));

      expect(() => creditGiftCard(null as unknown as GiftCardId, new Money(10))).toThrow();
      expect(() => creditGiftCard(cardId, null as unknown as Money)).toThrow();
    });
  });
});
