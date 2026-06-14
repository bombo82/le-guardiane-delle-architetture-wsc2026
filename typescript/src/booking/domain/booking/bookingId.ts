// Identità dell'aggregato Booking.

import { requireArgument } from '@/common/utils/requireArgument.js';
import type { AggregateId } from '@/common/domain/identity/aggregateId.js';
import { Uuid } from '@/common/domain/primitive/uuid.js';

export class BookingId implements AggregateId {
  readonly value: Uuid;

  constructor(value: Uuid) {
    requireArgument(value, 'BookingId value');
    this.value = value;
  }
}
