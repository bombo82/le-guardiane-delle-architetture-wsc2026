import { describe, expect, it } from 'vitest';
import { generateId } from '@/common/domain/identity/entityId.js';
import { Money } from '@/common/domain/primitive/money.js';
import { bookingRefused } from '@/booking/domain/events/bookingResultEvents.js';
import { BookingId } from '@/booking/domain/booking/bookingId.js';
import { refundRequestFromBookingRefused } from '@/booking/application/integration/payment/adapter/refundRequest.js';

describe('RefundRequest (booking -> payment)', () => {
  it('adapts BookingRefused to RefundRequestIntegrationCommand', () => {
    const bookingId = generateId((value) => new BookingId(value));
    const giftCardReference = crypto.randomUUID();
    const amount = new Money(30);
    const event = bookingRefused(bookingId, giftCardReference, amount);

    const command = refundRequestFromBookingRefused(event);

    expect(command.clientReference).toBe(bookingId.value.value);
    expect(command.amount).toBe(amount);
  });

  it('throws on null event', () => {
    expect(() => refundRequestFromBookingRefused(null as unknown as ReturnType<typeof bookingRefused>)).toThrow();
  });
});
