// Punto di ingresso dell'applicazione Express.

import express, { type ErrorRequestHandler } from 'express';
import swaggerUi from 'swagger-ui-express';
import { ApplicationModule } from './common/module/applicationModule.js';
import { Application } from './application.js';
import { DependencyNotProvidedError } from './common/errors/dependencyNotProvidedError.js';
import { IllegalArgumentError } from './common/errors/illegalArgumentError.js';
import { openapiSpecification } from './common/api/openapi.js';
import Database from 'better-sqlite3';

function initializeDb(moduleName: string): Database.Database {
  return ApplicationModule.initializeDb(moduleName);
}

function parsePort(args: string[]): number {
  if (args.length === 0) {
    return 7070;
  }
  const port = Number(args[0]);
  if (Number.isNaN(port) || port < 1 || port > 65535) {
    throw new Error(`Invalid port: ${args[0]}`);
  }
  return port;
}

function main(): void {
  const port = parsePort(process.argv.slice(2));

  const bookingDatabase = initializeDb('booking');
  const giftCardDatabase = initializeDb('giftcard');
  const paymentDatabase = initializeDb('payment');

  const application = new Application(bookingDatabase, giftCardDatabase, paymentDatabase);

  const app = express();
  app.use(express.json());

  app.get('/openapi', (_req, res) => res.json(openapiSpecification));
  app.use('/swagger', swaggerUi.serve, swaggerUi.setup(openapiSpecification));

  application.configure(app);

  const errorHandler: ErrorRequestHandler = (err, _req, res, next) => {
    if (err instanceof SyntaxError && 'body' in err) {
      res.status(400).send('request body is required');
      return;
    }
    if (err instanceof IllegalArgumentError) {
      res.status(400).json({ error: err.message });
      return;
    }
    if (err instanceof DependencyNotProvidedError) {
      res.status(500).json({ error: err.message });
      return;
    }
    next(err);
  };
  app.use(errorHandler);

  const server = app.listen(port, () => {
    console.log(`Application started on http://localhost:${port}`);
    console.log(' - BookingModule registered');
    console.log(' - GiftCardModule registered');
    console.log(' - PaymentModule registered');
  });

  const shutdown = (): void => {
    application.stop();
    server.close(() => {
      process.exit(0);
    });
  };

  process.on('SIGTERM', shutdown);
  process.on('SIGINT', shutdown);
}

main();
