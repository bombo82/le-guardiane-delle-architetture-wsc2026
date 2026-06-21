// Read model per una gift card esposta dall'API.

import type { Money } from '@/common/domain/primitive/money.js';

export type GiftCardDetails = {
  readonly id: string;
  readonly balance: Money;
};
