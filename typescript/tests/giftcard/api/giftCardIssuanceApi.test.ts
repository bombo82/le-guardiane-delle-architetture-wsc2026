import { beforeAll, describe, expect, it } from 'vitest';
import express from 'express';
import request from 'supertest';
import { GiftCardModule } from '@/giftcard/module.js';
import { DatabaseSetup } from '../../testsupport/databaseSetup.js';

describe('GiftCardIssuanceApi', () => {
  const app = express();
  let module: GiftCardModule;

  beforeAll(() => {
    const database = DatabaseSetup.initializeFileDb('giftcard', 'GiftCardIssuanceApiTest');
    module = new GiftCardModule(database, []);
    module.configure(app);
  });

  describe('post issuance', () => {
    it('should return 201', async () => {
      const response = await request(app).post('/gift-cards');

      expect(response.status).toEqual(201);
    });

    it('should return location header', async () => {
      const response = await request(app).post('/gift-cards');

      expect(response.headers['location']).toMatch(/\/gift-cards\/[0-9a-fA-F-]{36}/);
    });

    it('should return created card', async () => {
      const response = await request(app).post('/gift-cards');

      expect(response.body.id).not.toBeNull();
      expect(response.body.balance).toEqual(0);
    });
  });

  describe('getById', () => {
    it('should return 200', async () => {
      const created = await request(app).post('/gift-cards');
      const location = created.headers['location'] as string;

      const response = await request(app).get(location);

      expect(response.status).toEqual(200);
    });

    it('should return card', async () => {
      const created = await request(app).post('/gift-cards');
      const createdBody = created.body as { id: string; balance: number };
      const location = created.headers['location'] as string;

      const response = await request(app).get(location);

      expect(response.body.id).toEqual(createdBody.id);
      expect(response.body.balance).toEqual(0);
    });

    it('should return 404', async () => {
      const response = await request(app).get(`/gift-cards/${crypto.randomUUID()}`);
      expect(response.status).toEqual(404);
    });

    it('should return 400', async () => {
      const response = await request(app).get('/gift-cards/invalid-uuid');
      expect(response.status).toEqual(400);
      expect(response.text).toEqual('Invalid gift card id format');
    });
  });
});
