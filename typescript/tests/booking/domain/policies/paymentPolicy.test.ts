import { beforeEach, describe, expect, it } from 'vitest';
import { generateId } from '@/common/domain/identity/entityId.js';
import { Description } from '@/common/domain/primitive/description.js';
import { Money } from '@/common/domain/primitive/money.js';
import { Booking } from '@/booking/domain/booking/booking.js';
import { BookingId } from '@/booking/domain/booking/bookingId.js';
import { PaymentPolicy } from '@/booking/domain/policies/paymentPolicy.js';
import { SqliteBookingRepository } from '@/booking/infrastructure/sqliteBookingRepository.js';
import { GiftCardId } from '@/giftcard/domain/giftcard/giftCardId.js';
import { DatabaseSetup } from '../../../testsupport/databaseSetup.js';
import { PaymentAggregateFactory } from '../../../testsupport/payment/aggregateFactory.js';
import {
  paymentAccepted,
  paymentRejected,
  paymentExpired,
} from '@/payment/domain/events/paymentResultEvents.js';

describe('PaymentPolicy', () => {
  let bookingRepository: SqliteBookingRepository;
  let policy: PaymentPolicy;

  beforeEach(() => {
    const database = DatabaseSetup.initializeInMemoryDb('booking');
    bookingRepository = new SqliteBookingRepository(database);
    policy = new PaymentPolicy(bookingRepository);
  });

  it('evaluate on payment accepted returns confirm booking with gift card id from booking', () => {
    const bookingId = generateId((value) => new BookingId(value));
    const giftCardId = generateId((value) => new GiftCardId(value));
    saveBooking(bookingId, giftCardId);

    const payment = PaymentAggregateFactory.createPayment(bookingId.value.value, new Money(50));
    const amount = new Money(50);
    const event = paymentAccepted(
      payment.id(),
      payment.clientReference(),
      amount
    );

    const result = policy.evaluate(event);

    expect(result).not.toBeNull();
    expect(result?.kind).toBe('ConfirmBooking');
    expect(result?.aggregateId).toEqual(bookingId);
    expect(result?.giftCardId).toEqual(giftCardId);
    expect(result?.amount).toEqual(amount);
  });

  it('evaluate on payment accepted with non gift card provider returns confirm booking with gift card id from booking', () => {
    const bookingId = generateId((value) => new BookingId(value));
    const giftCardId = generateId((value) => new GiftCardId(value));
    saveBooking(bookingId, giftCardId);

    const payment = PaymentAggregateFactory.createPayment(bookingId.value.value, new Money(50));
    const amount = new Money(50);
    const event = paymentAccepted(
      payment.id(),
      payment.clientReference(),
      amount
    );

    const result = policy.evaluate(event);

    expect(result).not.toBeNull();
    expect(result?.kind).toBe('ConfirmBooking');
    expect(result?.aggregateId).toEqual(bookingId);
    expect(result?.giftCardId).toEqual(giftCardId);
    expect(result?.amount).toEqual(amount);
  });

  it('evaluate on payment rejected returns reject booking with gift card id from booking', () => {
    const bookingId = generateId((value) => new BookingId(value));
    const giftCardId = generateId((value) => new GiftCardId(value));
    saveBooking(bookingId, giftCardId);

    const payment = PaymentAggregateFactory.createPayment(bookingId.value.value, new Money(50));
    const event = paymentRejected(
      payment.id(),
      payment.clientReference(),
      payment.amount(),
      new Description('declined')
    );

    const result = policy.evaluate(event);

    expect(result).not.toBeNull();
    expect(result?.kind).toBe('RejectBooking');
    expect(result?.aggregateId).toEqual(bookingId);
    expect(result?.giftCardId).toEqual(giftCardId);
    expect(result?.amount).toEqual(payment.amount());
  });

  it('evaluate on payment accepted with client reference not matching any booking returns null', () => {
    const payment = PaymentAggregateFactory.createPayment(crypto.randomUUID(), new Money(50));
    const amount = new Money(50);
    const event = paymentAccepted(
      payment.id(),
      payment.clientReference(),
      amount
    );

    const result = policy.evaluate(event);

    expect(result).toBeNull();
  });

  it('evaluate on payment rejected with client reference not matching any booking returns null', () => {
    const payment = PaymentAggregateFactory.createPayment(crypto.randomUUID(), new Money(50));
    const event = paymentRejected(
      payment.id(),
      payment.clientReference(),
      payment.amount(),
      new Description('declined')
    );

    const result = policy.evaluate(event);

    expect(result).toBeNull();
  });

  it('evaluate on payment expired returns null', () => {
    const payment = PaymentAggregateFactory.createPayment();
    const event = paymentExpired(
      payment.id(),
      payment.clientReference(),
      payment.amount()
    );

    const result = policy.evaluate(event);

    expect(result).toBeNull();
  });

  it('evaluate on null event throws exception', () => {
    expect(() => policy.evaluate(null as unknown as Parameters<typeof policy.evaluate>[0])).toThrow();
  });

  function saveBooking(bookingId: BookingId, giftCardId: GiftCardId): void {
    const booking = Booking.place(bookingId, new Description('Test booking'), giftCardId);
    bookingRepository.save(booking);
  }
});
