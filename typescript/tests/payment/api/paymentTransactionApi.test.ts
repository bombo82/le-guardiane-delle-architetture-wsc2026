import { afterAll, beforeAll, describe, expect, it } from 'vitest';
import express from 'express';
import request from 'supertest';
import { PaymentModule } from '@/payment/module.js';
import { DatabaseSetup } from '../../testsupport/databaseSetup.js';

describe('PaymentTransactionApi', () => {
  const app = express();
  let module: PaymentModule;

  beforeAll(() => {
    const database = DatabaseSetup.initializeFileDb('payment', 'PaymentTransactionApiTest');
    module = new PaymentModule(database);
    module.webApis().forEach((api) => api.configure(app));
  });

  afterAll(() => {
    module.stop();
  });

  async function createPayment(clientReference: string, amount: number | string): Promise<string> {
    const body = {
      paymentId: crypto.randomUUID(),
      clientReference,
      amount,
      requestedAt: new Date().toISOString(),
    };
    const response = await request(app).post('/internals/payments').send(body);
    expect(response.status).toEqual(201);
    return response.headers['location'] as string;
  }

  function transactionRequest(provider: string, providerReference: string | null, amount: number | string) {
    if (providerReference !== null) {
      return { provider, providerReference, amount };
    }
    return { provider, amount };
  }

  it('should accept with PayPal', async () => {
    const clientReference = crypto.randomUUID();
    const location = await createPayment(clientReference, '50.00');
    const providerReference = crypto.randomUUID();

    const response = await request(app)
      .post(`${location}/transactions`)
      .send(transactionRequest('PayPal', providerReference, '50.00'));

    expect(response.status).toEqual(202);
    expect(response.body.provider).toEqual('PayPal');
    expect(response.body.providerReference).toEqual(providerReference);
    expect(response.body.amount).toEqual(50);
    expect(response.body.status).toEqual('STARTED');
  });

  it('should accept with Klarna', async () => {
    const clientReference = crypto.randomUUID();
    const location = await createPayment(clientReference, '75.00');
    const providerReference = crypto.randomUUID();

    const response = await request(app)
      .post(`${location}/transactions`)
      .send(transactionRequest('Klarna', providerReference, '75.00'));

    expect(response.status).toEqual(202);
    expect(response.body.provider).toEqual('Klarna');
    expect(response.body.providerReference).toEqual(providerReference);
    expect(response.body.amount).toEqual(75);
    expect(response.body.status).toEqual('STARTED');
  });

  it('should reject with GiftCard no reference', async () => {
    const clientReference = crypto.randomUUID();
    const location = await createPayment(clientReference, '30.00');

    const response = await request(app)
      .post(`${location}/transactions`)
      .send(transactionRequest('GiftCard', null, '30.00'));

    expect(response.status).toEqual(202);
    expect(response.body.provider).toEqual('GiftCard');
    expect(response.body.providerReference).toBeNull();
    expect(response.body.amount).toEqual(30);
    expect(response.body.status).toEqual('STARTED');
  });

  it('should accept with GiftCard and reference', async () => {
    const clientReference = crypto.randomUUID();
    const location = await createPayment(clientReference, '30.00');
    const providerReference = crypto.randomUUID();

    const response = await request(app)
      .post(`${location}/transactions`)
      .send(transactionRequest('GiftCard', providerReference, '30.00'));

    expect(response.status).toEqual(202);
    expect(response.body.provider).toEqual('GiftCard');
    expect(response.body.providerReference).toEqual(providerReference);
    expect(response.body.amount).toEqual(30);
    expect(response.body.status).toEqual('STARTED');
  });

  it('should return payment with transaction after start', async () => {
    const clientReference = crypto.randomUUID();
    const location = await createPayment(clientReference, '40.00');

    const transactionResponse = await request(app)
      .post(`${location}/transactions`)
      .send(transactionRequest('PayPal', crypto.randomUUID(), '40.00'));

    expect(transactionResponse.status).toEqual(202);
    const transaction = transactionResponse.body;

    const paymentResponse = await request(app).get(location);
    expect(paymentResponse.status).toEqual(200);

    expect(paymentResponse.body.transactions).toHaveLength(1);
    expect(paymentResponse.body.transactions[0].id).toEqual(transaction.id);
    expect(paymentResponse.body.transactions[0].provider).toEqual('PayPal');
  });

  it('should fail if provider missing', async () => {
    const clientReference = crypto.randomUUID();
    const location = await createPayment(clientReference, '50.00');

    const response = await request(app).post(`${location}/transactions`).send({ amount: 50 });

    expect(response.status).toEqual(400);
    expect(response.text).toEqual('provider is required');
  });

  it('should fail if amount missing', async () => {
    const clientReference = crypto.randomUUID();
    const location = await createPayment(clientReference, '50.00');

    const response = await request(app).post(`${location}/transactions`).send({ provider: 'PayPal' });

    expect(response.status).toEqual(400);
    expect(response.text).toEqual('amount is required');
  });

  it('should fail if payment id invalid', async () => {
    const response = await request(app)
      .post('/payments/invalid-uuid/transactions')
      .send(transactionRequest('PayPal', crypto.randomUUID(), '10.00'));

    expect(response.status).toEqual(400);
    expect(response.text).toEqual('Invalid payment id format');
  });

  it('should fail if payment not found', async () => {
    const response = await request(app)
      .post(`/payments/${crypto.randomUUID()}/transactions`)
      .send(transactionRequest('PayPal', crypto.randomUUID(), '10.00'));

    expect(response.status).toEqual(404);
  });

  it('should fail if provider unknown', async () => {
    const clientReference = crypto.randomUUID();
    const location = await createPayment(clientReference, '50.00');

    const response = await request(app)
      .post(`${location}/transactions`)
      .send(transactionRequest('Unknown', crypto.randomUUID(), '50.00'));

    expect(response.status).toEqual(400);
  });
});
