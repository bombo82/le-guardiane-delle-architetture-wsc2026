// GiftCard Bounded Context: modulo applicativo con wiring manuale.

import type { Express } from 'express';
import Database from 'better-sqlite3';
import { ApplicationModule } from '@/common/module/applicationModule.js';
import { GiftCardApi } from './api/giftCardApi.js';
import { BookingResultCrediting } from './application/services/bookingResultCrediting.js';
import { BookingResultRefunding } from './application/services/bookingResultRefunding.js';
import { TopUpConfirmation } from './application/services/topUpConfirmation.js';
import { GiftCardCrediting } from './application/usecases/giftCardCrediting.js';
import { GiftCardIssuing } from './application/usecases/giftCardIssuing.js';
import { GiftCardRefunding } from './application/usecases/giftCardRefunding.js';
import { TopUpConfirming } from './application/usecases/topUpConfirming.js';
import { TopUpRequesting } from './application/usecases/topUpRequesting.js';
import { GiftCardTopUpRequested } from './domain/events/giftCardTopUpRequested.js';
import { ConfirmTopUpPolicy } from './domain/policies/confirmTopUpPolicy.js';
import { CreditGiftCardPolicy } from './domain/policies/creditGiftCardPolicy.js';
import { RefundGiftCardPolicy } from './domain/policies/refundGiftCardPolicy.js';
import { TopUpPaymentRequestPolicy } from './domain/policies/topUpPaymentRequestPolicy.js';
import { InMemoryGiftCardEventBus } from './infrastructure/inMemoryGiftCardEventBus.js';
import { SqliteGiftCardRepository } from './infrastructure/sqliteGiftCardRepository.js';
import { requireDependency } from '@/common/utils/requireDependency.js';

export type TopUpRequestedHandler = (event: GiftCardTopUpRequested) => void;

export class GiftCardModule extends ApplicationModule {
  private readonly _giftCardRepository: SqliteGiftCardRepository;
  private readonly _eventBus: InMemoryGiftCardEventBus;
  private readonly _topUpConfirmation: TopUpConfirmation;
  private readonly _bookingResultCrediting: BookingResultCrediting;
  private readonly _bookingResultRefunding: BookingResultRefunding;
  private readonly _topUpPaymentRequestPolicy: TopUpPaymentRequestPolicy;
  private readonly _topUpRequestedHandlers: TopUpRequestedHandler[];

  constructor(database: Database.Database, topUpRequestedHandlers: TopUpRequestedHandler[] = []) {
    super();

    requireDependency(database, "database");

    this._topUpRequestedHandlers = [...topUpRequestedHandlers];
    this._database = database;
    this._giftCardRepository = new SqliteGiftCardRepository(this._database);
    this._eventBus = new InMemoryGiftCardEventBus((task) => task());
    this.registerCrossBcHandlers();
    this._topUpConfirmation = this.createTopUpConfirmation();
    this._bookingResultCrediting = this.createBookingResultCrediting();
    this._bookingResultRefunding = this.createBookingResultRefunding();
    this._topUpPaymentRequestPolicy = new TopUpPaymentRequestPolicy();
  }

  private readonly _database: Database.Database;

  topUpConfirmation(): TopUpConfirmation {
    return this._topUpConfirmation;
  }

  bookingResultCrediting(): BookingResultCrediting {
    return this._bookingResultCrediting;
  }

  bookingResultRefunding(): BookingResultRefunding {
    return this._bookingResultRefunding;
  }

  topUpPaymentRequestPolicy(): TopUpPaymentRequestPolicy {
    return this._topUpPaymentRequestPolicy;
  }

  configure(app: Express): void {
    const giftCardIssuing = new GiftCardIssuing(this._giftCardRepository);
    const topUpRequesting = new TopUpRequesting(this._giftCardRepository, this._eventBus);

    const api = new GiftCardApi(giftCardIssuing, this._giftCardRepository, topUpRequesting);
    api.configure(app);
  }

  private registerCrossBcHandlers(): void {
    for (const handler of this._topUpRequestedHandlers) {
      this._eventBus.subscribe('GiftCardTopUpRequested', {
        on: (event) => {
          if (event.kind === 'GiftCardTopUpRequested') {
            handler(event);
          }
        },
      });
    }
  }

  private createTopUpConfirmation(): TopUpConfirmation {
    const policy = new ConfirmTopUpPolicy();
    const useCase = new TopUpConfirming(this._giftCardRepository);
    return new TopUpConfirmation(policy, useCase);
  }

  private createBookingResultCrediting(): BookingResultCrediting {
    const policy = new CreditGiftCardPolicy();
    const useCase = new GiftCardCrediting(this._giftCardRepository);
    return new BookingResultCrediting(policy, useCase);
  }

  private createBookingResultRefunding(): BookingResultRefunding {
    const policy = new RefundGiftCardPolicy();
    const useCase = new GiftCardRefunding(this._giftCardRepository);
    return new BookingResultRefunding(policy, useCase);
  }
}
