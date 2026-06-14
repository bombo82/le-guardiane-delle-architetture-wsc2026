// Test E2E: flusso GiftCard top-up -> Payment.

import { beforeAll, afterAll, describe, it } from 'vitest';
import { Application } from '../../src/application.js';
import { DatabaseSetup } from '../testsupport/databaseSetup.js';
import { ExpressTestHelper } from '../testsupport/expressTestHelper.js';
import { E2EApiDriver } from './support/e2eApiDriver.js';

describe('GiftCardTopUpToPaymentE2E', () => {
  const topUpAmount = 50;

  const helper = new ExpressTestHelper();
  let application: Application;
  let api: E2EApiDriver;

  beforeAll(async () => {
    const bookingDatabase = DatabaseSetup.initializeFileDb('booking', 'GiftCardTopUpToPaymentE2E');
    const giftCardDatabase = DatabaseSetup.initializeFileDb('giftcard', 'GiftCardTopUpToPaymentE2E');
    const paymentDatabase = DatabaseSetup.initializeFileDb('payment', 'GiftCardTopUpToPaymentE2E');

    application = new Application(bookingDatabase, giftCardDatabase, paymentDatabase);
    api = new E2EApiDriver(helper);

    await helper.start((app) => application.configure(app));
  });

  afterAll(async () => {
    await helper.stop();
    application.stop();
  });

  it('topUpShouldCreatePaymentRequest', async () => {
    const giftCard = await api.issueGiftCard();

    await api.requestTopUp(giftCard.id, topUpAmount);

    const payment = await api.findPaymentRequestedFor(giftCard.id);
    api.assertPaymentMatches(payment, payment.id, giftCard.id, topUpAmount);

    const summary = await api.getPaymentSummary(payment.id);
    expect(summary.id).toBe(payment.id);
    expect(summary.clientReference).toBe(giftCard.id);
    expect(summary.amount).toBe(topUpAmount);
    expect(summary.status).toBe('REQUESTED');
  });

  it('payingTopUpWithPayPalShouldCreditGiftCardBalance', async () => {
    const giftCard = await api.issueGiftCard();
    const giftCardId = giftCard.id;

    await api.requestTopUp(giftCardId, topUpAmount);

    const payment = await api.findPaymentRequestedFor(giftCardId);
    await api.startFullPaymentTransaction(payment.id, 'PayPal', topUpAmount);

    const updatedCard = await api.waitForGiftCardBalanceToEqual(giftCardId, topUpAmount);
    expect(updatedCard.balance).toBe(topUpAmount);
  });
});
