// GiftCard Bounded Context: modulo applicativo con wiring manuale.

import type { Express } from 'express';
import Database from 'better-sqlite3';
import { ApplicationModule } from '@/common/module/applicationModule.js';
import { GiftCardApi } from './api/giftCardApi.js';
import { GiftCardQueryService } from './application/query/giftCardQueryService.js';
import { CreditFromBooking } from './application/integration/booking/handlers/creditFromBooking.js';
import { RefundFromBooking } from './application/integration/booking/handlers/refundFromBooking.js';
import { ConfirmTopUpFromPayment } from './application/integration/payment/handlers/confirmTopUpFromPayment.js';
import { GiftCardCrediting } from './application/usecases/giftCardCrediting.js';
import { GiftCardIssuing } from './application/usecases/giftCardIssuing.js';
import { GiftCardRefunding } from './application/usecases/giftCardRefunding.js';
import { TopUpConfirming } from './application/usecases/topUpConfirming.js';
import { TopUpRequesting } from './application/usecases/topUpRequesting.js';
import { paymentRequestFromTopUp } from './application/integration/payment/adapter/paymentRequest.js';
import { BookingResult } from './application/integration/booking/adapter/bookingResult.js';
import { PaymentResult } from './application/integration/payment/adapter/paymentResult.js';
import { InMemoryGiftCardEventBus } from './infrastructure/inMemoryGiftCardEventBus.js';
import type { GiftCardRepository } from './domain/ports/giftCardRepository.js';
import { SqliteGiftCardRepository } from './infrastructure/sqliteGiftCardRepository.js';
import type { PaymentResultIntegrationEvent } from '@/payment/integration/paymentResultIntegrationEvent.js';
import type { PaymentRequestIntegrationCommand } from '@/payment/integration/paymentRequestIntegrationCommand.js';
import type {
  BookingCompletedIntegrationEvent,
  BookingRefusedIntegrationEvent,
  BookingRejectedIntegrationEvent,
} from '@/booking/integration/giftcard/bookingResultIntegrationEvent.js';
import { requireDependency } from '@/common/utils/requireDependency.js';

export class GiftCardModule extends ApplicationModule {
  private readonly _giftCardRepository: GiftCardRepository;
  private readonly _eventBus: InMemoryGiftCardEventBus;
  private readonly _bookingResult: BookingResult;
  private readonly _paymentResult: PaymentResult;
  private readonly _creditFromBooking: CreditFromBooking;
  private readonly _refundFromBooking: RefundFromBooking;
  private readonly _confirmTopUpFromPayment: ConfirmTopUpFromPayment;

  constructor(database: Database.Database) {
    super();

    requireDependency(database, "database");

    this._giftCardRepository = new SqliteGiftCardRepository(database);
    this._eventBus = new InMemoryGiftCardEventBus((task) => task());
    this._bookingResult = new BookingResult();
    this._paymentResult = new PaymentResult();
    this._creditFromBooking = this.createCreditFromBooking();
    this._refundFromBooking = this.createRefundFromBooking();
    this._confirmTopUpFromPayment = this.createConfirmTopUpFromPayment();
  }

  onPaymentResult(event: PaymentResultIntegrationEvent): void {
    this._confirmTopUpFromPayment.handle(event);
  }

  onTopUpRequested(handler: (command: PaymentRequestIntegrationCommand) => void): void {
    this._eventBus.subscribe('GiftCardTopUpRequested', {
      on: (event) => {
        if (event.kind === 'GiftCardTopUpRequested') {
          handler(paymentRequestFromTopUp(event));
        }
      },
    });
  }

  onBookingCompleted(event: BookingCompletedIntegrationEvent): void {
    this._creditFromBooking.handle(event);
  }

  onBookingRefused(event: BookingRefusedIntegrationEvent): void {
    this._creditFromBooking.handle(event);
  }

  onBookingRejected(event: BookingRejectedIntegrationEvent): void {
    this._refundFromBooking.handle(event);
  }

  configure(app: Express): void {
    const giftCardIssuing = new GiftCardIssuing(this._giftCardRepository);
    const topUpRequesting = new TopUpRequesting(this._giftCardRepository, this._eventBus);
    const giftCardQueryService = new GiftCardQueryService(this._giftCardRepository);

    const api = new GiftCardApi(giftCardIssuing, giftCardQueryService, topUpRequesting);
    api.configure(app);
  }

  private createCreditFromBooking(): CreditFromBooking {
    const useCase = new GiftCardCrediting(this._giftCardRepository);
    return new CreditFromBooking(this._bookingResult, useCase);
  }

  private createRefundFromBooking(): RefundFromBooking {
    const useCase = new GiftCardRefunding(this._giftCardRepository);
    return new RefundFromBooking(this._bookingResult, useCase);
  }

  private createConfirmTopUpFromPayment(): ConfirmTopUpFromPayment {
    const useCase = new TopUpConfirming(this._giftCardRepository);
    return new ConfirmTopUpFromPayment(this._paymentResult, useCase);
  }
}
