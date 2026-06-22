// Composition root condiviso tra main.ts e i test E2E.
// Accetta esplicitamente i database per mantenere i test isolati.

import type { Express } from 'express';
import Database from 'better-sqlite3';
import { BookingModule } from './booking/module.js';
import { BookingResultEvent } from './booking/domain/events/bookingResultEvents.js';
import { BookingPaymentRequestPolicy } from './booking/application/policies/bookingPaymentRequestPolicy.js';
import { BookingRefundRequestPolicy } from './booking/application/policies/bookingRefundRequestPolicy.js';
import { GiftCardModule } from './giftcard/module.js';
import { PaymentModule } from './payment/module.js';
import { RefundTransaction } from './payment/application/commands/refundTransaction.js';
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
    // Gli handler cross-BC vengono aggiunti dopo la costruzione per rompere i cicli.
    this._paymentModule = new PaymentModule(paymentDatabase);
    this._giftCardModule = new GiftCardModule(giftCardDatabase);
    this._bookingModule = new BookingModule(bookingDatabase);

    this.wireTopUpRequests();
    this.wireBookingResults();
    this.wirePaymentResults();
  }

  private wirePaymentResults(): void {
    this._paymentModule.onPaymentResult((event) => this._bookingModule.handlePaymentResultFromPayment().handle(event));
    this._paymentModule.onPaymentResult((event) => this._giftCardModule.confirmTopUpFromPayment().handle(event));
  }

  private wireBookingResults(): void {
    this._bookingModule.onBookingResultIntegration((event) => this._giftCardModule.creditFromBooking().handle(event));
    this._bookingModule.onBookingRejectedIntegration((event) => this._giftCardModule.refundFromBooking().handle(event));
  }

  private wireTopUpRequests(): void {
    this._giftCardModule.onTopUpRequested((event) => {
      const command: RequestPayment = this._giftCardModule.topUpPaymentRequestPolicy().evaluate(event);
      this._paymentModule.paymentRequesting().invoke(command);
    });

    const bookingPaymentRequestPolicy = new BookingPaymentRequestPolicy();
    this._bookingModule.onBookingPlaced((event) => {
      const command: RequestPayment = bookingPaymentRequestPolicy.evaluate(event);
      this._paymentModule.paymentRequesting().invoke(command);
    });

    const bookingRefundRequestPolicy = new BookingRefundRequestPolicy(this._paymentModule.paymentRepository());
    this._bookingModule.onBookingResult((event: BookingResultEvent) => {
      if (event.kind === 'BookingRefused') {
        const refundCommand: RefundTransaction = bookingRefundRequestPolicy.evaluate(event);
        this._paymentModule.refundRequesting().invoke(refundCommand);
      }
    });
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
