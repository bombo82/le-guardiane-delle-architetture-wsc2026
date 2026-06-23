import { afterAll, beforeAll, describe, expect, it } from 'vitest';
import express from 'express';
import request from 'supertest';
import { BookingModule } from '@/booking/module.js';
import { DatabaseSetup } from '../../testsupport/databaseSetup.js';

describe('PlaceBookingApi', () => {
  const app = express();
  let module: BookingModule;

  beforeAll(() => {
    const database = DatabaseSetup.initializeFileDb('booking', 'PlaceBookingApiTest');
    module = new BookingModule(database);
    module.webApis().forEach((api) => api.configure(app));
  });

  afterAll(() => {
    module.stop();
  });

  function requestBody(amount: string, description: string): object {
    return {
      amount,
      description,
      giftCardId: crypto.randomUUID(),
    };
  }

  describe('post place', () => {
    it('should return 201', async () => {
      const response = await request(app).post('/bookings').send(requestBody('99.99', 'Nice trip'));

      expect(response.status).toEqual(201);
    });

    it('should return location header', async () => {
      const response = await request(app).post('/bookings').send(requestBody('50.00', 'Happy journey'));

      expect(response.headers['location']).toMatch(/\/bookings\/[0-9a-fA-F-]{36}/);
    });

    it('should return created booking', async () => {
      const response = await request(app).post('/bookings').send(requestBody('123.45', 'Adventure awaits'));

      expect(response.body.id).not.toBeNull();
      expect(response.body.description).toEqual('Adventure awaits');
      expect(response.body.giftCardId).not.toBeNull();
    });

    it('should fail if amount missing', async () => {
      const response = await request(app)
        .post('/bookings')
        .send({ description: 'missing amount', giftCardId: crypto.randomUUID() });

      expect(response.status).toEqual(400);
      expect(response.text).toEqual('amount is required');
    });

    it('should fail if description missing', async () => {
      const response = await request(app)
        .post('/bookings')
        .send({ amount: '100.00', giftCardId: crypto.randomUUID() });

      expect(response.status).toEqual(400);
      expect(response.text).toEqual('description is required');
    });

    it('should fail if gift card id missing', async () => {
      const response = await request(app)
        .post('/bookings')
        .send({ amount: '100.00', description: 'missing gift card' });

      expect(response.status).toEqual(400);
      expect(response.text).toEqual('giftCardId is required');
    });

    it('should fail if body missing', async () => {
      const response = await request(app).post('/bookings').send('');

      expect(response.status).toEqual(400);
      expect(response.text).toEqual('request body is required');
    });

    it('should fail if amount is negative', async () => {
      const response = await request(app)
        .post('/bookings')
        .send({ amount: '-10.00', description: 'Negative test', giftCardId: crypto.randomUUID() });

      expect(response.status).toEqual(400);
      expect(response.text).toContain('value must not be negative');
    });
  });

  describe('getById', () => {
    it('should return 200', async () => {
      const postResponse = await request(app).post('/bookings').send(requestBody('10.00', 'Get test'));
      const location = postResponse.headers['location'] as string;

      const response = await request(app).get(location);

      expect(response.status).toEqual(200);
    });

    it('should return booking', async () => {
      const postResponse = await request(app).post('/bookings').send(requestBody('10.00', 'Round trip'));
      const createdBody = postResponse.body as { id: string; description: string; giftCardId: string };
      const location = postResponse.headers['location'] as string;

      const response = await request(app).get(location);

      expect(response.body.id).toEqual(createdBody.id);
      expect(response.body.description).toEqual(createdBody.description);
      expect(response.body.giftCardId).toEqual(createdBody.giftCardId);
    });

    it('should return 404', async () => {
      const response = await request(app).get(`/bookings/${crypto.randomUUID()}`);
      expect(response.status).toEqual(404);
    });

    it('should return 400', async () => {
      const response = await request(app).get('/bookings/invalid-uuid');
      expect(response.status).toEqual(400);
      expect(response.text).toEqual('Invalid booking id format');
    });
  });
});
