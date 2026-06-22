// Payment Bounded Context: modulo applicativo con wiring manuale.

import type { Express, ErrorRequestHandler } from 'express';
import Database from 'better-sqlite3';
import { ApplicationModule } from '@/common/module/applicationModule.js';
import { PaymentApi } from './api/paymentApi.js';
import { PaymentInternalApi } from './api/paymentInternalApi.js';
import { PaymentFinder } from './application/query/paymentFinder.js';
import { PaymentProcessing } from './application/services/paymentProcessing.js';
import { RefundHandling } from './application/services/refundHandling.js';
import { PaymentExpiring } from './application/usecases/paymentExpiring.js';
import { PaymentRequesting } from './application/usecases/paymentRequesting.js';
import { RefundRequesting } from './application/usecases/refundRequesting.js';
import { TransactionAccepting } from './application/usecases/transactionAccepting.js';
import { TransactionRejecting } from './application/usecases/transactionRejecting.js';
import type { PaymentAccepted, PaymentRejected, PaymentExpired } from './domain/events/paymentResultEvents.js';
import {
  paymentAcceptedIntegrationEvent,
  paymentExpiredIntegrationEvent,
  paymentRejectedIntegrationEvent,
  type PaymentAcceptedIntegrationEvent,
  type PaymentExpiredIntegrationEvent,
  type PaymentRejectedIntegrationEvent,
  type PaymentResultIntegrationEvent,
} from './integration/paymentResultIntegrationEvent.js';
import { PaymentCompletion } from './application/policies/paymentCompletion.js';
import { PaymentExpiration } from './application/policies/paymentExpiration.js';
import { PaymentRejection } from './application/policies/paymentRejection.js';
import { TransactionRefund } from './application/policies/transactionRefund.js';
import { Provider } from './domain/payment/provider.js';
import type { PaymentProvider } from './domain/ports/paymentProvider.js';
import type { PaymentRepository } from './domain/ports/paymentRepository.js';
import { InMemoryPaymentEventBus } from './infrastructure/inMemoryPaymentEventBus.js';
import { PaymentDeadlineWatcher } from './infrastructure/paymentDeadlineWatcher.js';
import { SqlitePaymentRepository } from './infrastructure/sqlitePaymentRepository.js';
import { GiftCard } from './infrastructure/providers/giftCard.js';
import { Klarna } from './infrastructure/providers/klarna.js';
import { PayPal } from './infrastructure/providers/payPal.js';
import { requireArgument } from '@/common/utils/requireArgument.js';
import { requireDependency } from '@/common/utils/requireDependency.js';

type AcceptedHandler = (event: PaymentAccepted) => void;
type RejectedHandler = (event: PaymentRejected) => void;
type ExpiredHandler = (event: PaymentExpired) => void;
type AcceptedIntegrationHandler = (event: PaymentAcceptedIntegrationEvent) => void;
type RejectedIntegrationHandler = (event: PaymentRejectedIntegrationEvent) => void;
type ExpiredIntegrationHandler = (event: PaymentExpiredIntegrationEvent) => void;

export class PaymentModule extends ApplicationModule {
  private readonly _database: Database.Database;
  private readonly _paymentRepository: PaymentRepository;
  private readonly _eventBus: InMemoryPaymentEventBus;
  private readonly _paymentRequesting: PaymentRequesting;
  private readonly _refundRequesting: RefundRequesting;
  private readonly _acceptedHandlers: AcceptedHandler[];
  private readonly _rejectedHandlers: RejectedHandler[];
  private readonly _expiredHandlers: ExpiredHandler[];
  private readonly _acceptedIntegrationHandlers: AcceptedIntegrationHandler[];
  private readonly _rejectedIntegrationHandlers: RejectedIntegrationHandler[];
  private readonly _expiredIntegrationHandlers: ExpiredIntegrationHandler[];
  private _watcher: PaymentDeadlineWatcher | null = null;

  constructor(
    database: Database.Database,
    acceptedHandlers: AcceptedHandler[] = [],
    rejectedHandlers: RejectedHandler[] = [],
    expiredHandlers: ExpiredHandler[] = []
  ) {
    super();

    requireDependency(database, "database");

    this._acceptedHandlers = [...acceptedHandlers];
    this._rejectedHandlers = [...rejectedHandlers];
    this._expiredHandlers = [...expiredHandlers];
    this._acceptedIntegrationHandlers = [];
    this._rejectedIntegrationHandlers = [];
    this._expiredIntegrationHandlers = [];

    this._database = database;
    this._paymentRepository = new SqlitePaymentRepository(this._database);
    this._eventBus = new InMemoryPaymentEventBus((task) => setImmediate(task));
    this._paymentRequesting = new PaymentRequesting(this._paymentRepository, this._eventBus);
    this._refundRequesting = new RefundRequesting(this._paymentRepository, this._eventBus);
    this.registerCrossBcResultHandlers();
  }

  private registerCrossBcResultHandlers(): void {
    this._eventBus.subscribe('PaymentAccepted', {
      on: (event) => {
        if (event.kind === 'PaymentAccepted') {
          this._acceptedHandlers.forEach((handler) => handler(event));
          const integrationEvent = paymentAcceptedIntegrationEvent(
            event.clientReference.value.value,
            event.amount
          );
          this._acceptedIntegrationHandlers.forEach((handler) => handler(integrationEvent));
        }
      },
    });
    this._eventBus.subscribe('PaymentRejected', {
      on: (event) => {
        if (event.kind === 'PaymentRejected') {
          this._rejectedHandlers.forEach((handler) => handler(event));
          const integrationEvent = paymentRejectedIntegrationEvent(
            event.clientReference.value.value,
            event.amount,
            event.reason.value
          );
          this._rejectedIntegrationHandlers.forEach((handler) => handler(integrationEvent));
        }
      },
    });
    this._eventBus.subscribe('PaymentExpired', {
      on: (event) => {
        if (event.kind === 'PaymentExpired') {
          this._expiredHandlers.forEach((handler) => handler(event));
          const integrationEvent = paymentExpiredIntegrationEvent(
            event.clientReference.value.value,
            event.amount
          );
          this._expiredIntegrationHandlers.forEach((handler) => handler(integrationEvent));
        }
      },
    });
  }

  paymentRepository(): PaymentRepository {
    return this._paymentRepository;
  }

  paymentRequesting(): PaymentRequesting {
    return this._paymentRequesting;
  }

  refundRequesting(): RefundRequesting {
    return this._refundRequesting;
  }

  addAcceptedHandler(handler: AcceptedHandler): void {
    requireArgument(handler, 'handler');
    this._acceptedHandlers.push(handler);
  }

  addRejectedHandler(handler: RejectedHandler): void {
    requireArgument(handler, 'handler');
    this._rejectedHandlers.push(handler);
  }

  addExpiredHandler(handler: ExpiredHandler): void {
    requireArgument(handler, 'handler');
    this._expiredHandlers.push(handler);
  }

  onPaymentAccepted(handler: (event: PaymentAcceptedIntegrationEvent) => void): void {
    requireArgument(handler, 'handler');
    this._acceptedIntegrationHandlers.push(handler);
  }

  onPaymentRejected(handler: (event: PaymentRejectedIntegrationEvent) => void): void {
    requireArgument(handler, 'handler');
    this._rejectedIntegrationHandlers.push(handler);
  }

  onPaymentExpired(handler: (event: PaymentExpiredIntegrationEvent) => void): void {
    requireArgument(handler, 'handler');
    this._expiredIntegrationHandlers.push(handler);
  }

  publish(event: PaymentResultIntegrationEvent): void {
    switch (event.kind) {
      case 'PaymentAcceptedIntegrationEvent':
        this._acceptedIntegrationHandlers.forEach((handler) => handler(event));
        break;
      case 'PaymentRejectedIntegrationEvent':
        this._rejectedIntegrationHandlers.forEach((handler) => handler(event));
        break;
      case 'PaymentExpiredIntegrationEvent':
        this._expiredIntegrationHandlers.forEach((handler) => handler(event));
        break;
    }
  }

  configure(app: Express): void {
    const paymentFinder = new PaymentFinder(this._paymentRepository);

    const paymentCompletion = new PaymentCompletion();
    const paymentRejection = new PaymentRejection();
    const paymentExpiration = new PaymentExpiration();
    const transactionRefund = new TransactionRefund();

    const transactionAccepting = new TransactionAccepting(this._paymentRepository, this._eventBus, paymentCompletion);
    const transactionRejecting = new TransactionRejecting(this._paymentRepository, this._eventBus, paymentRejection);
    const paymentExpiring = new PaymentExpiring(this._paymentRepository, this._eventBus, paymentExpiration);

    this._eventBus.subscribe('TransactionAccepted', transactionAccepting);
    this._eventBus.subscribe('TransactionRejected', transactionRejecting);
    this._eventBus.subscribe('PaymentDeadlineReached', paymentExpiring);

    const providers: Record<string, PaymentProvider> = {
      [Provider.PAYPAL]: new PayPal(),
      [Provider.KLARNA]: new Klarna(),
      [Provider.GIFT_CARD]: new GiftCard(),
    };

    const refundHandling = new RefundHandling(this._paymentRepository, providers, transactionRefund, this._eventBus);
    this._eventBus.subscribe('RefundRequested', refundHandling);

    const paymentProcessing = new PaymentProcessing(this._paymentRepository, providers, this._eventBus);

    const api = new PaymentApi(paymentFinder, paymentProcessing);
    api.configure(app);

    const internalApi = new PaymentInternalApi(this._paymentRequesting, paymentFinder);
    internalApi.configure(app);

    const errorHandler: ErrorRequestHandler = (err, _req, res, next) => {
      if (err instanceof SyntaxError && 'body' in err) {
        res.status(400).send('request body is required');
        return;
      }
      next(err);
    };
    app.use(errorHandler);

    this._watcher = new PaymentDeadlineWatcher(this._paymentRepository, paymentExpiration, this._eventBus);
    this._watcher.start();
  }

  stop(): void {
    this._watcher?.stop();
  }
}
