import { describe, expect, it } from 'vitest';
import { generateId } from '@/common/domain/identity/entityId.js';
import { Description } from '@/common/domain/primitive/description.js';
import { Money } from '@/common/domain/primitive/money.js';
import { bookingPlaced } from '@/booking/domain/events/bookingPlaced.js';
import { BookingId } from '@/booking/domain/booking/bookingId.js';
import { paymentRequestFromBookingPlaced } from '@/booking/application/integration/payment/adapter/paymentRequest.js';

describe('PaymentRequest (booking -> payment)', () => {
  it('adapts BookingPlaced to PaymentRequestIntegrationCommand', () => {
    const bookingId = generateId((value) => new BookingId(value));
    const amount = new Money(75);
    const description = new Description('suite');
    const giftCardReference = crypto.randomUUID();
    const event = bookingPlaced(bookingId, amount, description, giftCardReference);

    const command = paymentRequestFromBookingPlaced(event);

    expect(command.clientReference).toBe(bookingId.value.value);
    expect(command.amount).toBe(amount);
  });

  it('throws on null event', () => {
    expect(() => paymentRequestFromBookingPlaced(null as unknown as ReturnType<typeof bookingPlaced>)).toThrow();
  });
});
