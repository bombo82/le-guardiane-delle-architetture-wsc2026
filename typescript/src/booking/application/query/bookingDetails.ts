// Read model per una prenotazione esposta dall'API.

import type { Description } from '@/common/domain/primitive/description.js';

export type BookingDetails = {
  readonly id: string;
  readonly description: Description;
  readonly giftCardId: string;
};
