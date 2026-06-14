// Test E2E: flusso Booking -> GiftCard credit.

import { beforeAll, afterAll, describe, it } from 'vitest';
import { Application } from '../../src/application.js';
import { DatabaseSetup } from '../testsupport/databaseSetup.js';
import { ExpressTestHelper } from '../testsupport/expressTestHelper.js';
import { E2EApiDriver } from './support/e2eApiDriver.js';

describe('BookingToGiftCardCreditE2E', () => {
  const bookingAmount = 75;

  const helper = new ExpressTestHelper();
  let application: Application;
  let api: E2EApiDriver;

  beforeAll(async () => {
    const bookingDatabase = DatabaseSetup.initializeFileDb('booking', 'BookingToGiftCardCreditE2E');
    const giftCardDatabase = DatabaseSetup.initializeFileDb('giftcard', 'BookingToGiftCardCreditE2E');
    const paymentDatabase = DatabaseSetup.initializeFileDb('payment', 'BookingToGiftCardCreditE2E');

    application = new Application(bookingDatabase, giftCardDatabase, paymentDatabase);
    api = new E2EApiDriver(helper);

    await helper.start((app) => application.configure(app));
  });

  afterAll(async () => {
    await helper.stop();
    application.stop();
  });

  it('placingBookingShouldCreatePaymentRequest', async () => {
    const giftCard = await api.issueGiftCard();
    const booking = await api.placeBooking(giftCard.id, bookingAmount);

    const payment = await api.findPaymentRequestedFor(booking.id);
    api.assertPaymentMatches(payment, payment.id, booking.id, bookingAmount);

    const summary = await api.getPaymentSummary(payment.id);
    expect(summary.id).toBe(payment.id);
    expect(summary.clientReference).toBe(booking.id);
    expect(summary.amount).toBe(bookingAmount);
    expect(summary.status).toBe('REQUESTED');
  });

  it('payingBookingWithKlarnaShouldCreditGiftCardBalance', async () => {
    const giftCard = await api.issueGiftCard();
    const booking = await api.placeBooking(giftCard.id, bookingAmount);

    const payment = await api.findPaymentRequestedFor(booking.id);
    await api.startFullPaymentTransaction(payment.id, 'Klarna', bookingAmount);

    const updatedCard = await api.waitForGiftCardBalanceToEqual(giftCard.id, bookingAmount);
    expect(updatedCard.balance).toBe(bookingAmount);
  });
});
