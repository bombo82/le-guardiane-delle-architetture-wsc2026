import { afterAll, beforeAll, describe, expect, it } from 'vitest';
import express, { type Express } from 'express';
import request from 'supertest';
import http from 'node:http';
import swaggerUi from 'swagger-ui-express';
import { openapiSpecification } from '@/common/api/openapi.js';

describe('OpenApiSpec', () => {
  let app: Express;
  let server: http.Server;

  beforeAll(() => {
    app = express();
    app.use(express.json());

    app.get('/openapi', (_req, res) => res.json(openapiSpecification));
    app.use('/swagger', swaggerUi.serve, swaggerUi.setup(openapiSpecification));

    server = app.listen(0);
  });

  afterAll(() => {
    server.close();
  });

  describe('GET /openapi', () => {
    it('should return 200 with OpenAPI JSON', async () => {
      const response = await request(app).get('/openapi');

      expect(response.status).toEqual(200);
      expect(response.body.openapi).toEqual('3.0.0');
      expect(response.body.info.title).toEqual('WSC2026 API');
      expect(response.body.paths).toHaveProperty('/bookings');
      expect(response.body.paths).toHaveProperty('/gift-cards');
      expect(response.body.paths).toHaveProperty('/payments/{id}');
      expect(response.body.paths).toHaveProperty('/internals/payments');
      expect(response.body.components.schemas).toHaveProperty('PlaceBookingRequest');
      expect(response.body.components.schemas).toHaveProperty('BookingResponse');
      expect(response.body.components.schemas).toHaveProperty('GiftCardResponse');
      expect(response.body.components.schemas).toHaveProperty('PaymentDetailsResponse');
    });
  });

  describe('GET /swagger', () => {
    it('should return 200 with Swagger UI HTML', async () => {
      const response = await request(app).get('/swagger').redirects(1);

      expect(response.status).toEqual(200);
      expect(response.headers['content-type']).toContain('text/html');
      expect(response.text).toContain('swagger-ui');
    });
  });
});
