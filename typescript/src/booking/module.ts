// Booking Bounded Context: modulo applicativo con wiring manuale.

import Database from 'better-sqlite3';
import { ApplicationModule, type WebApi } from '@/common/module/applicationModule.js';
import { BookingApi } from './api/bookingApi.js';
import { BookingQueryService } from './application/query/bookingQueryService.js';
import { HandlePaymentResultFromPayment } from './application/integration/payment/handlers/handlePaymentResultFromPayment.js';
import { BookingConfirming } from './application/usecases/bookingConfirming.js';
import { BookingPlacing } from './application/usecases/bookingPlacing.js';
import { BookingRejecting } from './application/usecases/bookingRejecting.js';
import { paymentRequestFromBookingPlaced } from './application/integration/payment/adapter/paymentRequest.js';
import { refundRequestFromBookingRefused } from './application/integration/payment/adapter/refundRequest.js';
import {
  bookingCompletedIntegrationEvent,
  bookingRefusedIntegrationEvent,
  bookingRejectedIntegrationEvent,
  type BookingCompletedIntegrationEvent,
  type BookingRefusedIntegrationEvent,
  type BookingRejectedIntegrationEvent,
} from './integration/giftcard/bookingResultIntegrationEvent.js';
import { PaymentResult } from './application/integration/payment/adapter/paymentResult.js';
import { InMemoryBookingEventBus } from './infrastructure/inMemoryBookingEventBus.js';
import type { BookingRepository } from './domain/ports/bookingRepository.js';
import { SqliteBookingRepository } from './infrastructure/sqliteBookingRepository.js';
import type { PaymentResultIntegrationEvent } from '@/payment/integration/paymentResultIntegrationEvent.js';
import type { PaymentRequestIntegrationCommand } from '@/payment/integration/paymentRequestIntegrationCommand.js';
import type { RefundRequestIntegrationCommand } from '@/payment/integration/refundRequestIntegrationCommand.js';
import { requireDependency } from '@/common/utils/requireDependency.js';

export class BookingModule extends ApplicationModule {
  private readonly _bookingRepository: BookingRepository;
  private readonly _eventBus: InMemoryBookingEventBus;
  private readonly _handlePaymentResultFromPayment: HandlePaymentResultFromPayment;

  constructor(database: Database.Database) {
    super();

    requireDependency(database, "database");

    this._bookingRepository = new SqliteBookingRepository(database);
    this._eventBus = new InMemoryBookingEventBus((task) => task());
    this._handlePaymentResultFromPayment = this.createHandlePaymentResultFromPayment();
  }

  handlePaymentResult(event: PaymentResultIntegrationEvent): void {
    this._handlePaymentResultFromPayment.handle(event);
  }

  onBookingPlaced(handler: (command: PaymentRequestIntegrationCommand) => void): void {
    this._eventBus.subscribe('BookingPlaced', {
      on: (event) => {
        if (event.kind === 'BookingPlaced') {
          handler(paymentRequestFromBookingPlaced(event));
        }
      },
    });
  }

  onBookingRefused(handler: (command: RefundRequestIntegrationCommand) => void): void {
    this._eventBus.subscribe('BookingRefused', {
      on: (event) => {
        if (event.kind === 'BookingRefused') {
          handler(refundRequestFromBookingRefused(event));
        }
      },
    });
  }

  onBookingCompletedIntegration(handler: (event: BookingCompletedIntegrationEvent) => void): void {
    this._eventBus.subscribe('BookingConfirmed', {
      on: (event) => {
        if (event.kind === 'BookingConfirmed') {
          handler(bookingCompletedIntegrationEvent(event.giftCardReference, event.amount));
        }
      },
    });
  }

  onBookingRefusedIntegration(handler: (event: BookingRefusedIntegrationEvent) => void): void {
    this._eventBus.subscribe('BookingRefused', {
      on: (event) => {
        if (event.kind === 'BookingRefused') {
          handler(bookingRefusedIntegrationEvent(event.giftCardReference, event.amount));
        }
      },
    });
  }

  onBookingRejectedIntegration(handler: (event: BookingRejectedIntegrationEvent) => void): void {
    this._eventBus.subscribe('BookingRejected', {
      on: (event) => {
        if (event.kind === 'BookingRejected') {
          handler(bookingRejectedIntegrationEvent(event.giftCardReference, event.amount));
        }
      },
    });
  }

  webApis(): WebApi[] {
    const bookingPlacing = new BookingPlacing(this._bookingRepository, this._eventBus);
    const bookingQueryService = new BookingQueryService(this._bookingRepository);
    return [new BookingApi(bookingPlacing, bookingQueryService)];
  }

  private createHandlePaymentResultFromPayment(): HandlePaymentResultFromPayment {
    const paymentResult = new PaymentResult(this._bookingRepository);
    const bookingConfirming = new BookingConfirming(this._bookingRepository, this._eventBus);
    const bookingRejecting = new BookingRejecting(this._bookingRepository, this._eventBus);
    return new HandlePaymentResultFromPayment(paymentResult, bookingConfirming, bookingRejecting);
  }
}
