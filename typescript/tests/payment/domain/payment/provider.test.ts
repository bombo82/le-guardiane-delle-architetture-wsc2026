import { describe, expect, it } from 'vitest';
import { Provider, providerFromLabel } from '@/payment/domain/payment/provider.js';

describe('Provider', () => {
  it('should resolve known providers', () => {
    expect(providerFromLabel('PayPal')).toEqual(Provider.PAYPAL);
    expect(providerFromLabel('Klarna')).toEqual(Provider.KLARNA);
    expect(providerFromLabel('GiftCard')).toEqual(Provider.GIFT_CARD);
  });

  it('should fail for null label', () => {
    expect(() => providerFromLabel(null as unknown as string)).toThrow();
  });

  it('should fail for unknown label', () => {
    expect(() => providerFromLabel('Unknown')).toThrow();
  });
});
