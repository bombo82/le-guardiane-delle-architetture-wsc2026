import { afterAll, beforeAll, describe, expect, it } from 'vitest';
import express from 'express';
import request from 'supertest';
import { PaymentModule } from '@/payment/module.js';
import { DatabaseSetup } from '../../testsupport/databaseSetup.js';

describe('PaymentRequestApi', () => {
  const app = express();
  let module: PaymentModule;

  beforeAll(() => {
    const database = DatabaseSetup.initializeFileDb('payment', 'PaymentRequestApiTest');
    module = new PaymentModule(database);
    module.webApis().forEach((api) => api.configure(app));
  });

  afterAll(() => {
    module.stop();
  });

  function createPayment(clientReference: string, amount: number | string) {
    const body = {
      paymentId: crypto.randomUUID(),
      clientReference,
      amount,
      requestedAt: new Date().toISOString(),
    };
    return request(app).post('/internals/payments').send(body);
  }

  describe('getById', () => {
    it('should return 200', async () => {
      const clientReference = crypto.randomUUID();
      const created = await createPayment(clientReference, '50.00');
      expect(created.status).toEqual(201);

      const response = await request(app).get(`/payments/${created.body.id}`);

      expect(response.status).toEqual(200);
    });

    it('should return payment', async () => {
      const clientReference = crypto.randomUUID();
      const created = await createPayment(clientReference, '50.00');

      const response = await request(app).get(`/payments/${created.body.id}`);

      expect(response.body.id).toEqual(created.body.id);
      expect(response.body.clientReference).toEqual(clientReference);
      expect(response.body.status).toEqual('REQUESTED');
      expect(response.body.amount).toEqual(50);
    });

    it('should return 404', async () => {
      const response = await request(app).get(`/payments/${crypto.randomUUID()}`);
      expect(response.status).toEqual(404);
    });

    it('should return 400', async () => {
      const response = await request(app).get('/payments/invalid-uuid');
      expect(response.status).toEqual(400);
      expect(response.text).toEqual('Invalid payment id format');
    });
  });

  describe('internalCreatePayment', () => {
    it('should return 201', async () => {
      const clientReference = crypto.randomUUID();

      const response = await createPayment(clientReference, '50.00');

      expect(response.status).toEqual(201);
      expect(response.headers['location']).toMatch(/\/payments\/[0-9a-fA-F-]{36}/);
    });

    it('should return 400 when body missing', async () => {
      const response = await request(app).post('/internals/payments').send('');
      expect(response.status).toEqual(400);
    });
  });
});
