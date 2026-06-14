import { requireArgument } from '@/common/utils/requireArgument.js';
// Provider di pagamento supportati.

export const Provider = {
  PAYPAL: 'PayPal',
  KLARNA: 'Klarna',
  GIFT_CARD: 'GiftCard',
} as const;

export type Provider = (typeof Provider)[keyof typeof Provider];

export function providerFromLabel(label: string): Provider {
  requireArgument(label, 'provider label');
  const value = Object.values(Provider).find((provider) => provider === label);
  if (value === undefined) {
    throw new Error(`unknown provider: ${label}`);
  }
  return value;
}
