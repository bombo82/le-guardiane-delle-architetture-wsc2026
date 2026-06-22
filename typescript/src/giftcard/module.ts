// GiftCard Bounded Context: modulo applicativo con wiring manuale.

import type { Express } from 'express';
import Database from 'better-sqlite3';
import { ApplicationModule } from '@/common/module/applicationModule.js';
import { GiftCardApi } from './api/giftCardApi.js';
import { GiftCardQueryService } from './application/query/giftCardQueryService.js';
import { TopUpConfirmation } from './application/services/topUpConfirmation.js';
import { CreditFromBooking } from './application/integration/booking/handlers/creditFromBooking.js';
import { RefundFromBooking } from './application/integration/booking/handlers/refundFromBooking.js';
import { GiftCardCrediting } from './application/usecases/giftCardCrediting.js';
import { GiftCardIssuing } from './application/usecases/giftCardIssuing.js';
import { GiftCardRefunding } from './application/usecases/giftCardRefunding.js';
import { TopUpConfirming } from './application/usecases/topUpConfirming.js';
import { TopUpRequesting } from './application/usecases/topUpRequesting.js';
import { GiftCardTopUpRequested } from './domain/events/giftCardTopUpRequested.js';
import { ConfirmTopUpPolicy } from './application/policies/confirmTopUpPolicy.js';
import { TopUpPaymentRequestPolicy } from './application/policies/topUpPaymentRequestPolicy.js';
import { BookingResult } from './application/integration/booking/adapter/bookingResult.js';
import { InMemoryGiftCardEventBus } from './infrastructure/inMemoryGiftCardEventBus.js';
import { SqliteGiftCardRepository } from './infrastructure/sqliteGiftCardRepository.js';
import { requireDependency } from '@/common/utils/requireDependency.js';

export type TopUpRequestedHandler = (event: GiftCardTopUpRequested) => void;

export class GiftCardModule extends ApplicationModule {
  private readonly _giftCardRepository: SqliteGiftCardRepository;
  private readonly _eventBus: InMemoryGiftCardEventBus;
  private readonly _topUpConfirmation: TopUpConfirmation;
  private readonly _bookingResult: BookingResult;
  private readonly _creditFromBooking: CreditFromBooking;
  private readonly _refundFromBooking: RefundFromBooking;
  private readonly _topUpPaymentRequestPolicy: TopUpPaymentRequestPolicy;
  private readonly _topUpRequestedHandlers: TopUpRequestedHandler[];

  constructor(database: Database.Database, topUpRequestedHandlers: TopUpRequestedHandler[] = []) {
    super();

    requireDependency(database, "database");

    this._topUpRequestedHandlers = [...topUpRequestedHandlers];
    this._database = database;
    this._giftCardRepository = new SqliteGiftCardRepository(this._database);
    this._eventBus = new InMemoryGiftCardEventBus((task) => task());
    this._bookingResult = new BookingResult();
    this.registerCrossBcHandlers();
    this._topUpConfirmation = this.createTopUpConfirmation();
    this._creditFromBooking = this.createCreditFromBooking();
    this._refundFromBooking = this.createRefundFromBooking();
    this._topUpPaymentRequestPolicy = new TopUpPaymentRequestPolicy();
  }

  private readonly _database: Database.Database;

  topUpConfirmation(): TopUpConfirmation {
    return this._topUpConfirmation;
  }

  creditFromBooking(): CreditFromBooking {
    return this._creditFromBooking;
  }

  refundFromBooking(): RefundFromBooking {
    return this._refundFromBooking;
  }

  topUpPaymentRequestPolicy(): TopUpPaymentRequestPolicy {
    return this._topUpPaymentRequestPolicy;
  }

  configure(app: Express): void {
    const giftCardIssuing = new GiftCardIssuing(this._giftCardRepository);
    const topUpRequesting = new TopUpRequesting(this._giftCardRepository, this._eventBus);
    const giftCardQueryService = new GiftCardQueryService(this._giftCardRepository);

    const api = new GiftCardApi(giftCardIssuing, giftCardQueryService, topUpRequesting);
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

  private createCreditFromBooking(): CreditFromBooking {
    const useCase = new GiftCardCrediting(this._giftCardRepository);
    return new CreditFromBooking(this._bookingResult, useCase);
  }

  private createRefundFromBooking(): RefundFromBooking {
    const useCase = new GiftCardRefunding(this._giftCardRepository);
    return new RefundFromBooking(this._bookingResult, useCase);
  }
}
