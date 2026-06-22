import { beforeAll, describe, expect, it } from 'vitest';
import { Money } from '@/common/domain/primitive/money.js';
import {
  paymentAcceptedIntegrationEvent,
  paymentExpiredIntegrationEvent,
  paymentRejectedIntegrationEvent,
} from '@/payment/integration/paymentResultIntegrationEvent.js';
import { HandlePaymentResultFromPayment } from '@/booking/application/integration/payment/handlers/handlePaymentResultFromPayment.js';
import { PaymentResult } from '@/booking/application/integration/payment/adapter/paymentResult.js';
import { BookingConfirming } from '@/booking/application/usecases/bookingConfirming.js';
import { BookingRejecting } from '@/booking/application/usecases/bookingRejecting.js';
import { SqliteBookingRepository } from '@/booking/infrastructure/sqliteBookingRepository.js';
import { InMemoryBookingEventBus } from '@/booking/infrastructure/inMemoryBookingEventBus.js';
import { DatabaseSetup } from '../../../../../testsupport/databaseSetup.js';
import { BookingAggregateFactory } from '../../../../../testsupport/booking/aggregateFactory.js';
import { Uuid } from '@/common/domain/primitive/uuid.js';

describe('HandlePaymentResultFromPayment', () => {
  let repository: SqliteBookingRepository;
  let handler: HandlePaymentResultFromPayment;

  beforeAll(() => {
    const database = DatabaseSetup.initializeInMemoryDb('booking');
    repository = new SqliteBookingRepository(database);
    const eventBus = new InMemoryBookingEventBus((task) => task());
    const paymentResult = new PaymentResult(repository);
    const confirming = new BookingConfirming(repository, eventBus);
    const rejecting = new BookingRejecting(repository, eventBus);
    handler = new HandlePaymentResultFromPayment(paymentResult, confirming, rejecting);
  });

  describe('construction', () => {
    it('rejects null parameters', () => {
      const paymentResult = new PaymentResult(repository);
      const eventBus = new InMemoryBookingEventBus((task) => task());
      const confirming = new BookingConfirming(repository, eventBus);
      const rejecting = new BookingRejecting(repository, eventBus);

      expect(() => new HandlePaymentResultFromPayment(null as unknown as PaymentResult, confirming, rejecting)).toThrow();
      expect(() => new HandlePaymentResultFromPayment(paymentResult, null as unknown as BookingConfirming, rejecting)).toThrow();
      expect(() => new HandlePaymentResultFromPayment(paymentResult, confirming, null as unknown as BookingRejecting)).toThrow();
    });
  });

  describe('payment results handling', () => {
    it('on accepted should confirm booking', () => {
      const booking = BookingAggregateFactory.createBooking();
      repository.save(booking);
      const amount = new Money(50);

      handler.handle(paymentAcceptedIntegrationEvent(booking.id().value.value, amount));

      const updated = repository.findById(booking.id());
      expect(updated).not.toBeNull();
      expect(updated!.status()).toBe('CONFIRMED');
    });

    it('on rejected should reject booking', () => {
      const booking = BookingAggregateFactory.createBooking();
      repository.save(booking);
      const amount = new Money(50);

      handler.handle(paymentRejectedIntegrationEvent(booking.id().value.value, amount, 'declined'));

      const updated = repository.findById(booking.id());
      expect(updated).not.toBeNull();
      expect(updated!.status()).toBe('REJECTED');
    });

    it('on expired should do nothing', () => {
      const booking = BookingAggregateFactory.createBooking();
      repository.save(booking);

      handler.handle(paymentExpiredIntegrationEvent(booking.id().value.value, new Money(50)));

      const updated = repository.findById(booking.id());
      expect(updated).not.toBeNull();
      expect(updated!.status()).toBe('PLACED');
    });

    it('on accepted unknown booking should do nothing', () => {
      const amount = new Money(50);

      handler.handle(paymentAcceptedIntegrationEvent(Uuid.generate().value, amount));

      expect(true).toBe(true);
    });
  });
});
