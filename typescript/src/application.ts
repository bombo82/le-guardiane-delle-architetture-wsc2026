// Composition root condiviso tra main.ts e i test E2E.
// Accetta esplicitamente i database per mantenere i test isolati.

import type { Express } from 'express';
import Database from 'better-sqlite3';
import { BookingModule } from './booking/module.js';
import { BookingPlaced } from './booking/domain/events/bookingPlaced.js';
import type { BookingResultEvent } from './booking/domain/events/bookingResultEvents.js';
import type {
  BookingRejectedIntegrationEvent,
  BookingResultIntegrationEvent,
} from './booking/integration/giftcard/bookingResultIntegrationEvent.js';
import { BookingPaymentRequestPolicy } from './booking/application/policies/bookingPaymentRequestPolicy.js';
import { BookingRefundRequestPolicy } from './booking/application/policies/bookingRefundRequestPolicy.js';
import { GiftCardModule } from './giftcard/module.js';
import { GiftCardTopUpRequested } from './giftcard/domain/events/giftCardTopUpRequested.js';
import { PaymentModule } from './payment/module.js';
import type { RefundTransaction } from './payment/application/commands/refundTransaction.js';
import { RequestPayment } from './payment/application/commands/requestPayment.js';
import { requireDependency } from '@/common/utils/requireDependency.js';

export class Application {
  private readonly _bookingModule: BookingModule;
  private readonly _giftCardModule: GiftCardModule;
  private readonly _paymentModule: PaymentModule;

  constructor(
    bookingDatabase: Database.Database,
    giftCardDatabase: Database.Database,
    paymentDatabase: Database.Database
  ) {
    requireDependency(bookingDatabase, "bookingDatabase");
    requireDependency(giftCardDatabase, "giftCardDatabase");
    requireDependency(paymentDatabase, "paymentDatabase");

    // PaymentModule prima: i suoi casi d'uso sono necessari al wiring degli altri moduli.
    // Gli handler cross-BC vengono aggiunti dopo la costruzione per rompere il ciclo.
    this._paymentModule = new PaymentModule(paymentDatabase, [], [], []);

    // AtomicReference risolve il ciclo di inizializzazione di GiftCardModule.
    const giftCardModuleRef: { current: GiftCardModule | null } = { current: null };
    const topUpToPaymentHandler = (event: GiftCardTopUpRequested): void => {
      if (giftCardModuleRef.current === null) {
        throw new Error('GiftCardModule not initialized');
      }
      const command: RequestPayment = giftCardModuleRef.current.topUpPaymentRequestPolicy().evaluate(event);
      this._paymentModule.paymentRequesting().invoke(command);
    };

    this._giftCardModule = new GiftCardModule(giftCardDatabase, [topUpToPaymentHandler]);
    giftCardModuleRef.current = this._giftCardModule;

    const bookingRefundRequestPolicy = new BookingRefundRequestPolicy(this._paymentModule.paymentRepository());
    const bookingPaymentRequestPolicy = new BookingPaymentRequestPolicy();

    const bookingPlacedHandler = (event: BookingPlaced): void => {
      const command: RequestPayment = bookingPaymentRequestPolicy.evaluate(event);
      this._paymentModule.paymentRequesting().invoke(command);
    };

    const bookingConfirmedHandler = (event: BookingResultEvent): void => {
      if (event.kind === 'BookingRefused') {
        const refundCommand: RefundTransaction = bookingRefundRequestPolicy.evaluate(event);
        this._paymentModule.refundRequesting().invoke(refundCommand);
      }
    };

    const bookingResultIntegrationHandler = (event: BookingResultIntegrationEvent): void => {
      this._giftCardModule.creditFromBooking().handle(event);
    };

    const bookingRejectedIntegrationHandler = (event: BookingRejectedIntegrationEvent): void => {
      this._giftCardModule.refundFromBooking().handle(event);
    };

    this._bookingModule = new BookingModule(
      bookingDatabase,
      [bookingPlacedHandler],
      [bookingConfirmedHandler],
      [],
      [bookingResultIntegrationHandler],
      [bookingRejectedIntegrationHandler]
    );

    this._paymentModule.addAcceptedHandler((event) => this._bookingModule.paymentResultOutcome().handlePaymentResults(event));
    this._paymentModule.addRejectedHandler((event) => this._bookingModule.paymentResultOutcome().handlePaymentResults(event));
    this._paymentModule.addExpiredHandler((event) => this._bookingModule.paymentResultOutcome().handlePaymentResults(event));

    this._paymentModule.addAcceptedHandler((event) => this._giftCardModule.topUpConfirmation().handlePaymentResults(event));
    this._paymentModule.addRejectedHandler((event) => this._giftCardModule.topUpConfirmation().handlePaymentResults(event));
    this._paymentModule.addExpiredHandler((event) => this._giftCardModule.topUpConfirmation().handlePaymentResults(event));
  }

  configure(app: Express): void {
    this._bookingModule.configure(app);
    this._giftCardModule.configure(app);
    this._paymentModule.configure(app);
  }

  stop(): void {
    this._bookingModule.stop();
    this._giftCardModule.stop();
    this._paymentModule.stop();
  }
}
