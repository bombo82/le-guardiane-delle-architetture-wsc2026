import { beforeAll, describe, expect, it } from 'vitest';
import express from 'express';
import request from 'supertest';
import { GiftCardModule } from '@/giftcard/module.js';
import { DatabaseSetup } from '../../testsupport/databaseSetup.js';

describe('GiftCardTopUpApi', () => {
  const app = express();
  let module: GiftCardModule;

  beforeAll(() => {
    const database = DatabaseSetup.initializeFileDb('giftcard', 'GiftCardTopUpApiTest');
    module = new GiftCardModule(database);
    module.configure(app);
  });

  async function createGiftCard(): Promise<string> {
    const response = await request(app).post('/gift-cards');
    expect(response.status).toEqual(201);
    return response.headers['location'] as string;
  }

  describe('requestTopUp', () => {
    it('should return 200', async () => {
      const location = await createGiftCard();

      const response = await request(app).post(`${location}/top-up`).send({ amount: 50 });

      expect(response.status).toEqual(200);
    });

    it('should return updated card', async () => {
      const location = await createGiftCard();

      const response = await request(app).post(`${location}/top-up`).send({ amount: 50 });

      expect(response.body.id).toEqual(location.substring(location.lastIndexOf('/') + 1));
      expect(response.body.balance).toEqual(0);
    });

    it('should return 404', async () => {
      const response = await request(app).post(`/gift-cards/${crypto.randomUUID()}/top-up`).send({ amount: 10 });
      expect(response.status).toEqual(404);
    });

    it('should fail if amount missing', async () => {
      const location = await createGiftCard();

      const response = await request(app).post(`${location}/top-up`).send({});

      expect(response.status).toEqual(400);
    });

    it('should fail if amount is invalid', async () => {
      const location = await createGiftCard();

      const response = await request(app).post(`${location}/top-up`).send({ amount: -10 });

      expect(response.status).toEqual(400);
    });

    it('should return 400 for invalid uuid', async () => {
      const response = await request(app).post('/gift-cards/invalid-uuid/top-up').send({ amount: 10 });
      expect(response.status).toEqual(400);
      expect(response.text).toEqual('Invalid gift card id format');
    });

    it('should fail if body missing', async () => {
      const location = await createGiftCard();

      const response = await request(app).post(`${location}/top-up`).send('');
      expect(response.status).toEqual(400);
      expect(response.text).toEqual('request body is required');
    });

    it('should fail if body is null', async () => {
      const location = await createGiftCard();

      const response = await request(app).post(`${location}/top-up`).send('null');
      expect(response.status).toEqual(400);
      expect(response.text).toEqual('request body is required');
    });
  });
});
