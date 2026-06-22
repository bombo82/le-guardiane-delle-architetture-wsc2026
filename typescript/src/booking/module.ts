// Booking Bounded Context: modulo applicativo con wiring manuale.

import type { Express } from 'express';
import Database from 'better-sqlite3';
import { ApplicationModule } from '@/common/module/applicationModule.js';
import { BookingApi } from './api/bookingApi.js';
import { BookingQueryService } from './application/query/bookingQueryService.js';
import { HandlePaymentResultFromPayment } from './application/integration/payment/handlers/handlePaymentResultFromPayment.js';
import { BookingConfirming } from './application/usecases/bookingConfirming.js';
import { BookingPlacing } from './application/usecases/bookingPlacing.js';
import { BookingRejecting } from './application/usecases/bookingRejecting.js';
import { BookingPlaced } from './domain/events/bookingPlaced.js';
import type { BookingResultEvent } from './domain/events/bookingResultEvents.js';
import {
  bookingCompletedIntegrationEvent,
  bookingRefusedIntegrationEvent,
  bookingRejectedIntegrationEvent,
  type BookingRejectedIntegrationEvent,
  type BookingResultIntegrationEvent,
} from './integration/giftcard/bookingResultIntegrationEvent.js';
import { PaymentResult } from './application/integration/payment/adapter/paymentResult.js';
import { InMemoryBookingEventBus } from './infrastructure/inMemoryBookingEventBus.js';
import { SqliteBookingRepository } from './infrastructure/sqliteBookingRepository.js';
import { requireDependency } from '@/common/utils/requireDependency.js';

export type BookingPlacedHandler = (event: BookingPlaced) => void;
export type BookingResultHandler = (event: BookingResultEvent) => void;
export type BookingResultIntegrationHandler = (event: BookingResultIntegrationEvent) => void;
export type BookingRejectedIntegrationHandler = (event: BookingRejectedIntegrationEvent) => void;

export class BookingModule extends ApplicationModule {
  private readonly _database: Database.Database;
  private readonly _bookingRepository: SqliteBookingRepository;
  private readonly _eventBus: InMemoryBookingEventBus;
  private readonly _handlePaymentResultFromPayment: HandlePaymentResultFromPayment;

  constructor(database: Database.Database) {
    super();

    requireDependency(database, "database");

    this._database = database;
    this._bookingRepository = new SqliteBookingRepository(this._database);
    this._eventBus = new InMemoryBookingEventBus((task) => task());
    this._handlePaymentResultFromPayment = this.createHandlePaymentResultFromPayment();
  }

  handlePaymentResultFromPayment(): HandlePaymentResultFromPayment {
    return this._handlePaymentResultFromPayment;
  }

  onBookingPlaced(handler: BookingPlacedHandler): void {
    this._eventBus.subscribe('BookingPlaced', {
      on: (event) => {
        if (event.kind === 'BookingPlaced') {
          handler(event);
        }
      },
    });
  }

  onBookingResult(handler: BookingResultHandler): void {
    this._eventBus.subscribe('BookingConfirmed', {
      on: (event) => {
        if (event.kind === 'BookingConfirmed') {
          handler(event);
        }
      },
    });
    this._eventBus.subscribe('BookingRefused', {
      on: (event) => {
        if (event.kind === 'BookingRefused') {
          handler(event);
        }
      },
    });
    this._eventBus.subscribe('BookingRejected', {
      on: (event) => {
        if (event.kind === 'BookingRejected') {
          handler(event);
        }
      },
    });
  }

  onBookingResultIntegration(handler: BookingResultIntegrationHandler): void {
    this._eventBus.subscribe('BookingConfirmed', {
      on: (event) => {
        if (event.kind === 'BookingConfirmed') {
          handler(bookingCompletedIntegrationEvent(event.giftCardReference, event.amount));
        }
      },
    });
    this._eventBus.subscribe('BookingRefused', {
      on: (event) => {
        if (event.kind === 'BookingRefused') {
          handler(bookingRefusedIntegrationEvent(event.giftCardReference, event.amount));
        }
      },
    });
  }

  onBookingRejectedIntegration(handler: BookingRejectedIntegrationHandler): void {
    this._eventBus.subscribe('BookingRejected', {
      on: (event) => {
        if (event.kind === 'BookingRejected') {
          handler(bookingRejectedIntegrationEvent(event.giftCardReference, event.amount));
        }
      },
    });
  }

  configure(app: Express): void {
    const bookingPlacing = new BookingPlacing(this._bookingRepository, this._eventBus);
    const bookingQueryService = new BookingQueryService(this._bookingRepository);

    const api = new BookingApi(bookingPlacing, bookingQueryService);
    api.configure(app);
  }

  private createHandlePaymentResultFromPayment(): HandlePaymentResultFromPayment {
    const paymentResult = new PaymentResult(this._bookingRepository);
    const bookingConfirming = new BookingConfirming(this._bookingRepository, this._eventBus);
    const bookingRejecting = new BookingRejecting(this._bookingRepository, this._eventBus);
    return new HandlePaymentResultFromPayment(paymentResult, bookingConfirming, bookingRejecting);
  }
}
