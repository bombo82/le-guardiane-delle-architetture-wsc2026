// Aggregato Booking: gestisce il ciclo di vita di una prenotazione.

// Refactor to use a published language (e.g., common types or primitive IDs).

import type { Aggregate } from '@/common/domain/model/aggregate.js';
import { Description } from '@/common/domain/primitive/description.js';
import { Money } from '@/common/domain/primitive/money.js';
import { GiftCardReference } from '../primitive/giftCardReference.js';
import type { BookingResultEvent, BookingRejected } from '../events/bookingResultEvents.js';
import { bookingConfirmed, bookingRefused, bookingRejected } from '../events/bookingResultEvents.js';
import { BookingId } from './bookingId.js';
import { BookingStatus } from './bookingStatus.js';
import { requireArgument } from '@/common/utils/requireArgument.js';

export class Booking implements Aggregate<BookingId> {
  private readonly _id: BookingId;
  private readonly _description: Description;
  private readonly _giftCardReference: GiftCardReference;
  private _status: BookingStatus;

  constructor(id: BookingId, description: Description, giftCardReference: GiftCardReference, status: BookingStatus) {
    requireArgument(id, 'id');
    requireArgument(description, 'description');
    requireArgument(giftCardReference, 'giftCardReference');
    requireArgument(status, 'status');

    this._id = id;
    this._description = description;
    this._giftCardReference = giftCardReference;
    this._status = status;
  }

  static place(id: BookingId, description: Description, giftCardReference: GiftCardReference): Booking {
    requireArgument(id, 'id');
    requireArgument(description, 'description');
    requireArgument(giftCardReference, 'giftCardReference');

    return new Booking(id, description, giftCardReference, BookingStatus.PLACED);
  }

  confirm(amount: Money): BookingResultEvent {
    requireArgument(amount, 'amount');

    // TODO: implement business rule for confirmation vs refusal
    // (e.g. check room availability, overbooking policy, etc.)
    const canConfirm = true;

    if (canConfirm) {
      this._status = BookingStatus.CONFIRMED;
      return bookingConfirmed(this._id, this._giftCardReference.value.value, amount);
    } else {
      this._status = BookingStatus.REFUSED;
      return bookingRefused(this._id, this._giftCardReference.value.value, amount);
    }
  }

  reject(amount: Money): BookingRejected {
    requireArgument(amount, 'amount');

    this._status = BookingStatus.REJECTED;
    return bookingRejected(this._id, this._giftCardReference.value.value, amount);
  }

  id(): BookingId {
    return this._id;
  }

  description(): Description {
    return this._description;
  }

  giftCardReference(): GiftCardReference {
    return this._giftCardReference;
  }

  status(): BookingStatus {
    return this._status;
  }
}
