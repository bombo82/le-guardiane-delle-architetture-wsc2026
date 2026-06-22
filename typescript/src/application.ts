// Composition root condiviso tra main.ts e i test E2E.
// Accetta esplicitamente i database per mantenere i test isolati.

import type { Express } from 'express';
import Database from 'better-sqlite3';
import { BookingModule } from './booking/module.js';
import { GiftCardModule } from './giftcard/module.js';
import { PaymentModule } from './payment/module.js';
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
    this._paymentModule.onPaymentResult((event) => this._bookingModule.onPaymentResult(event));
    this._paymentModule.onPaymentResult((event) => this._giftCardModule.onPaymentResult(event));
  }

  private wireBookingResults(): void {
    this._bookingModule.onBookingCompletedIntegration((event) => this._giftCardModule.onBookingCompleted(event));
    this._bookingModule.onBookingRefusedIntegration((event) => this._giftCardModule.onBookingRefused(event));
    this._bookingModule.onBookingRejectedIntegration((event) => this._giftCardModule.onBookingRejected(event));
  }

  private wireTopUpRequests(): void {
    this._giftCardModule.onTopUpRequested((command) => this._paymentModule.requestPayment(command));
    this._bookingModule.onBookingPlaced((command) => this._paymentModule.requestPayment(command));
    this._bookingModule.onBookingRefused((command) => this._paymentModule.requestRefund(command));
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
