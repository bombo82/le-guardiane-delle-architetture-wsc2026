// Configurazione centrale per la generazione della specifica OpenAPI.

import swaggerJsdoc from 'swagger-jsdoc';

const options: swaggerJsdoc.Options = {
  definition: {
    openapi: '3.0.0',
    info: {
      title: 'WSC2026 API',
      version: '1.0',
      description: 'API del workshop Le Guardiane delle Architetture (WSC2026)',
    },
  },
  apis: [
    'src/common/api/openapiSchemas.ts',
    'src/booking/api/*.ts',
    'src/giftcard/api/*.ts',
    'src/payment/api/*.ts',
  ],
};

export const openapiSpecification = swaggerJsdoc(options);
