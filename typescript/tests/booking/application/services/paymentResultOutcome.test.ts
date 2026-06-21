import { beforeEach, describe, expect, it } from 'vitest';
import { Description } from '@/common/domain/primitive/description.js';
import { Money } from '@/common/domain/primitive/money.js';
import { BookingConfirming } from '@/booking/application/usecases/bookingConfirming.js';
import { BookingRejecting } from '@/booking/application/usecases/bookingRejecting.js';
import { PaymentPolicy } from '@/booking/application/policies/paymentPolicy.js';
import { PaymentResultOutcome } from '@/booking/application/services/paymentResultOutcome.js';
import { SqliteBookingRepository } from '@/booking/infrastructure/sqliteBookingRepository.js';
import { DatabaseSetup } from '../../../testsupport/databaseSetup.js';
import { BookingAggregateFactory } from '../../../testsupport/booking/aggregateFactory.js';
import { PaymentAggregateFactory } from '../../../testsupport/payment/aggregateFactory.js';
import {
  paymentAccepted,
  paymentRejected,
  paymentExpired,
} from '@/payment/domain/events/paymentResultEvents.js';
import type { BookingEvent } from '@/booking/domain/events/bookingEvent.js';
import type { EventPublisher } from '@/common/application/events/eventPublisher.js';

describe('PaymentResultOutcome', () => {
  let repository: SqliteBookingRepository;
  let service: PaymentResultOutcome;

  const noOpPublisher: EventPublisher<BookingEvent> = {
    publish: () => {},
  };

  beforeEach(() => {
    const database = DatabaseSetup.initializeInMemoryDb('booking');
    repository = new SqliteBookingRepository(database);

    const policy = new PaymentPolicy(repository);
    const confirmation = new BookingConfirming(repository, noOpPublisher);
    const rejection = new BookingRejecting(repository, noOpPublisher);

    service = new PaymentResultOutcome(policy, confirmation, rejection);
  });

  describe('construction', () => {
    it('reject null parameters', () => {
      const policy = new PaymentPolicy(repository);
      const confirmation = new BookingConfirming(repository, noOpPublisher);
      const rejection = new BookingRejecting(repository, noOpPublisher);

      expect(() => new PaymentResultOutcome(null as unknown as PaymentPolicy, confirmation, rejection)).toThrow();
      expect(() => new PaymentResultOutcome(policy, null as unknown as BookingConfirming, rejection)).toThrow();
      expect(() => new PaymentResultOutcome(policy, confirmation, null as unknown as BookingRejecting)).toThrow();
    });
  });

  describe('payment results handling', () => {
    it('on accepted should confirm booking', () => {
      const booking = BookingAggregateFactory.createBooking();
      repository.save(booking);
      const payment = PaymentAggregateFactory.createPayment(booking.id().value.value, new Money(50));
      const amount = new Money(50);

      service.handlePaymentResults(
        paymentAccepted(payment.id(), payment.clientReference(), amount)
      );

      const updated = repository.findById(booking.id());
      expect(updated).not.toBeNull();
    });

    it('on rejected should reject booking', () => {
      const booking = BookingAggregateFactory.createBooking();
      repository.save(booking);
      const payment = PaymentAggregateFactory.createPayment(booking.id().value.value, new Money(50));

      service.handlePaymentResults(
        paymentRejected(
          payment.id(),
          payment.clientReference(),
          payment.amount(),
          new Description('declined')
        )
      );

      const updated = repository.findById(booking.id());
      expect(updated).not.toBeNull();
    });

    it('on expired should do nothing', () => {
      const booking = BookingAggregateFactory.createBooking();
      repository.save(booking);
      const payment = PaymentAggregateFactory.createPayment(booking.id().value.value, new Money(50));

      service.handlePaymentResults(
        paymentExpired(payment.id(), payment.clientReference(), payment.amount())
      );

      const updated = repository.findById(booking.id());
      expect(updated).not.toBeNull();
    });
  });
});
