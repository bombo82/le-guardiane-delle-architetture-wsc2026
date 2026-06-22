import { describe, expect, it } from 'vitest';
import { Money } from '@/common/domain/primitive/money.js';
import {
  paymentAcceptedIntegrationEvent,
  paymentExpiredIntegrationEvent,
  paymentRejectedIntegrationEvent,
} from '@/payment/integration/paymentResultIntegrationEvent.js';
import { PaymentResult } from '@/booking/application/integration/payment/adapter/paymentResult.js';
import { SqliteBookingRepository } from '@/booking/infrastructure/sqliteBookingRepository.js';
import { DatabaseSetup } from '../../../../../testsupport/databaseSetup.js';
import { BookingAggregateFactory } from '../../../../../testsupport/booking/aggregateFactory.js';
import { Uuid } from '@/common/domain/primitive/uuid.js';

describe('PaymentResult', () => {
  const database = DatabaseSetup.initializeInMemoryDb('booking');
  const repository = new SqliteBookingRepository(database);
  const paymentResult = new PaymentResult(repository);

  function saveBooking() {
    const booking = BookingAggregateFactory.createBooking();
    repository.save(booking);
    return booking;
  }

  it('adapts PaymentAccepted to ConfirmBooking', () => {
    const booking = saveBooking();
    const amount = new Money(50);
    const event = paymentAcceptedIntegrationEvent(booking.id().value.value, amount);

    const result = paymentResult.adapt(event);

    expect(result).not.toBeNull();
    expect(result!.kind).toBe('ConfirmBooking');
    expect(result!.aggregateId).toEqual(booking.id());
    expect(result!.giftCardReference).toEqual(booking.giftCardReference());
    expect(result!.amount).toEqual(amount);
  });

  it('adapts PaymentRejected to RejectBooking', () => {
    const booking = saveBooking();
    const amount = new Money(50);
    const event = paymentRejectedIntegrationEvent(booking.id().value.value, amount, 'declined');

    const result = paymentResult.adapt(event);

    expect(result).not.toBeNull();
    expect(result!.kind).toBe('RejectBooking');
    expect(result!.aggregateId).toEqual(booking.id());
    expect(result!.giftCardReference).toEqual(booking.giftCardReference());
    expect(result!.amount).toEqual(amount);
  });

  it('adapts PaymentExpired to null', () => {
    const booking = saveBooking();
    const event = paymentExpiredIntegrationEvent(booking.id().value.value, new Money(50));

    const result = paymentResult.adapt(event);

    expect(result).toBeNull();
  });

  it('adapts unknown booking to null', () => {
    const event = paymentAcceptedIntegrationEvent(Uuid.generate().value, new Money(50));

    const result = paymentResult.adapt(event);

    expect(result).toBeNull();
  });

  it('throws on null event', () => {
    expect(() => paymentResult.adapt(null as unknown as Parameters<typeof paymentResult.adapt>[0])).toThrow();
  });
});
