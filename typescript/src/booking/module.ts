// Booking Bounded Context: modulo applicativo con wiring manuale.

import type { Express } from 'express';
import Database from 'better-sqlite3';
import { ApplicationModule } from '@/common/module/applicationModule.js';
import { BookingApi } from './api/bookingApi.js';
import { BookingQueryService } from './application/query/bookingQueryService.js';
import { PaymentResultOutcome } from './application/services/paymentResultOutcome.js';
import { BookingConfirming } from './application/usecases/bookingConfirming.js';
import { BookingPlacing } from './application/usecases/bookingPlacing.js';
import { BookingRejecting } from './application/usecases/bookingRejecting.js';
import { BookingPlaced } from './domain/events/bookingPlaced.js';
import type { BookingResultEvent, BookingRejected } from './domain/events/bookingResultEvents.js';
import { PaymentPolicy } from './application/policies/paymentPolicy.js';
import { InMemoryBookingEventBus } from './infrastructure/inMemoryBookingEventBus.js';
import { SqliteBookingRepository } from './infrastructure/sqliteBookingRepository.js';
import { requireDependency } from '@/common/utils/requireDependency.js';

export type BookingPlacedHandler = (event: BookingPlaced) => void;
export type BookingResultHandler = (event: BookingResultEvent) => void;
export type BookingRejectedHandler = (event: BookingRejected) => void;

export class BookingModule extends ApplicationModule {
  private readonly _database: Database.Database;
  private readonly _bookingRepository: SqliteBookingRepository;
  private readonly _eventBus: InMemoryBookingEventBus;
  private readonly _paymentResultOutcome: PaymentResultOutcome;
  private readonly _bookingPlacedHandlers: BookingPlacedHandler[];
  private readonly _bookingConfirmedHandlers: BookingResultHandler[];
  private readonly _bookingRejectedHandlers: BookingRejectedHandler[];

  constructor(
    database: Database.Database,
    bookingPlacedHandlers: BookingPlacedHandler[] = [],
    bookingConfirmedHandlers: BookingResultHandler[] = [],
    bookingRejectedHandlers: BookingRejectedHandler[] = []
  ) {
    super();

    requireDependency(database, "database");

    this._database = database;
    this._bookingRepository = new SqliteBookingRepository(this._database);
    this._eventBus = new InMemoryBookingEventBus((task) => task());
    this._bookingPlacedHandlers = [...bookingPlacedHandlers];
    this._bookingConfirmedHandlers = [...bookingConfirmedHandlers];
    this._bookingRejectedHandlers = [...bookingRejectedHandlers];
    this.registerCrossBcHandlers();
    this._paymentResultOutcome = this.createPaymentResultOutcome();
  }

  paymentResultOutcome(): PaymentResultOutcome {
    return this._paymentResultOutcome;
  }

  configure(app: Express): void {
    const bookingPlacing = new BookingPlacing(this._bookingRepository, this._eventBus);
    const bookingQueryService = new BookingQueryService(this._bookingRepository);

    const api = new BookingApi(bookingPlacing, bookingQueryService);
    api.configure(app);
  }

  private createPaymentResultOutcome(): PaymentResultOutcome {
    const paymentPolicy = new PaymentPolicy(this._bookingRepository);
    const bookingConfirming = new BookingConfirming(this._bookingRepository, this._eventBus);
    const bookingRejecting = new BookingRejecting(this._bookingRepository, this._eventBus);
    return new PaymentResultOutcome(paymentPolicy, bookingConfirming, bookingRejecting);
  }

  private registerCrossBcHandlers(): void {
    for (const handler of this._bookingPlacedHandlers) {
      this._eventBus.subscribe('BookingPlaced', {
        on: (event) => {
          if (event.kind === 'BookingPlaced') {
            handler(event);
          }
        },
      });
    }

    for (const handler of this._bookingConfirmedHandlers) {
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
    }

    for (const handler of this._bookingRejectedHandlers) {
      this._eventBus.subscribe('BookingRejected', {
        on: (event) => {
          if (event.kind === 'BookingRejected') {
            handler(event);
          }
        },
      });
    }
  }
}
