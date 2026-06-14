// Aggregato Booking: gestisce il ciclo di vita di una prenotazione.

// Refactor to use a published language (e.g., common types or primitive IDs).

import type { Aggregate } from '@/common/domain/model/aggregate.js';
import { Description } from '@/common/domain/primitive/description.js';
import { Money } from '@/common/domain/primitive/money.js';
import { GiftCardId } from '@/giftcard/domain/giftcard/giftCardId.js';
import type { BookingResultEvent, BookingRejected } from '../events/bookingResultEvents.js';
import { bookingConfirmed, bookingRefused, bookingRejected } from '../events/bookingResultEvents.js';
import { BookingId } from './bookingId.js';
import { BookingStatus } from './bookingStatus.js';
import { requireArgument } from '@/common/utils/requireArgument.js';

export class Booking implements Aggregate<BookingId> {
  private readonly _id: BookingId;
  private readonly _description: Description;
  private readonly _giftCardId: GiftCardId;
  private _status: BookingStatus;

  constructor(id: BookingId, description: Description, giftCardId: GiftCardId, status: BookingStatus) {
    requireArgument(id, 'id');
    requireArgument(description, 'description');
    requireArgument(giftCardId, 'giftCardId');
    requireArgument(status, 'status');

    this._id = id;
    this._description = description;
    this._giftCardId = giftCardId;
    this._status = status;
  }

  static place(id: BookingId, description: Description, giftCardId: GiftCardId): Booking {
    requireArgument(id, 'id');
    requireArgument(description, 'description');
    requireArgument(giftCardId, 'giftCardId');

    return new Booking(id, description, giftCardId, BookingStatus.PLACED);
  }

  confirm(giftCardId: GiftCardId, amount: Money): BookingResultEvent {
    requireArgument(giftCardId, 'giftCardId');
    requireArgument(amount, 'amount');

    // TODO: implement business rule for confirmation vs refusal
    // (e.g. check room availability, overbooking policy, etc.)
    const canConfirm = true;

    if (canConfirm) {
      this._status = BookingStatus.CONFIRMED;
      return bookingConfirmed(this._id, giftCardId, amount);
    } else {
      this._status = BookingStatus.REFUSED;
      return bookingRefused(this._id, giftCardId, amount);
    }
  }

  reject(giftCardId: GiftCardId, amount: Money): BookingRejected {
    requireArgument(giftCardId, 'giftCardId');
    requireArgument(amount, 'amount');

    this._status = BookingStatus.REJECTED;
    return bookingRejected(this._id, giftCardId, amount);
  }

  id(): BookingId {
    return this._id;
  }

  description(): Description {
    return this._description;
  }

  giftCardId(): GiftCardId {
    return this._giftCardId;
  }

  status(): BookingStatus {
    return this._status;
  }
}
