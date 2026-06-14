// Driver API per i test E2E: nasconde i dettagli HTTP e fornisce helper di dominio.

import type { GiftCardResponse } from '../../../src/giftcard/api/giftCardResponse.js';
import type { BookingResponse } from '../../../src/booking/api/bookingResponse.js';
import type { PaymentDetailsResponse } from '../../../src/payment/api/paymentDetailsResponse.js';
import type { ExpressTestHelper } from '../../testsupport/expressTestHelper.js';

function sleep(millis: number): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, millis));
}

export class E2EApiDriver {
  private readonly _helper: ExpressTestHelper;

  constructor(helper: ExpressTestHelper) {
    this._helper = helper;
  }

  async issueGiftCard(): Promise<GiftCardResponse> {
    const response = await this._helper.post('/gift-cards');
    expect(response.status).toBe(201);
    const giftCard = JSON.parse(response.body) as GiftCardResponse;
    expect(giftCard.id).not.toBeNull();
    return giftCard;
  }

  async requestTopUp(giftCardId: string, amount: number): Promise<GiftCardResponse> {
    const response = await this._helper.post(`/gift-cards/${giftCardId}/top-up`, { amount });
    expect(response.status).toBe(200);
    return JSON.parse(response.body) as GiftCardResponse;
  }

  async placeBooking(giftCardId: string, amount: number): Promise<BookingResponse> {
    const response = await this._helper.post('/bookings', {
      amount,
      description: 'E2E booking',
      giftCardId,
    });
    expect(response.status).toBe(201);
    const booking = JSON.parse(response.body) as BookingResponse;
    expect(booking.id).not.toBeNull();
    expect(booking.giftCardId).toBe(giftCardId);
    return booking;
  }

  async startFullPaymentTransaction(paymentId: string, provider: string, amount: number): Promise<PaymentDetailsResponse> {
    const response = await this._helper.post(`/payments/${paymentId}/transactions`, {
      provider,
      providerReference: crypto.randomUUID(),
      amount,
    });
    expect(response.status).toBe(202);
    return JSON.parse(response.body) as PaymentDetailsResponse;
  }

  async findPaymentRequestedFor(clientReference: string): Promise<PaymentDetailsResponse> {
    const response = await this._helper.get(`/internals/payments?clientReference=${encodeURIComponent(clientReference)}`);
    expect(response.status).toBe(200);
    const payment = JSON.parse(response.body) as PaymentDetailsResponse;
    expect(payment.status).toBe('REQUESTED');
    return payment;
  }

  async getPaymentSummary(paymentId: string): Promise<PaymentDetailsResponse> {
    const response = await this._helper.get(`/payments/${paymentId}`);
    expect(response.status).toBe(200);
    return JSON.parse(response.body) as PaymentDetailsResponse;
  }

  async getGiftCardSummary(giftCardId: string): Promise<GiftCardResponse> {
    const response = await this._helper.get(`/gift-cards/${giftCardId}`);
    expect(response.status).toBe(200);
    return JSON.parse(response.body) as GiftCardResponse;
  }

  async waitForGiftCardBalanceToEqual(giftCardId: string, expectedBalance: number): Promise<GiftCardResponse> {
    const deadline = Date.now() + 2000;
    while (Date.now() < deadline) {
      const card = await this.getGiftCardSummary(giftCardId);
      if (card.balance === expectedBalance) {
        return card;
      }
      await sleep(100);
    }
    throw new Error(`Gift card balance was not updated to ${expectedBalance} within timeout`);
  }

  assertPaymentMatches(
    payment: PaymentDetailsResponse,
    expectedId: string,
    expectedClientReference: string,
    expectedAmount: number
  ): void {
    expect(payment.id).toBe(expectedId);
    expect(payment.clientReference).toBe(expectedClientReference);
    expect(payment.amount).toBe(expectedAmount);
    expect(payment.status).toBe('REQUESTED');
  }
}
