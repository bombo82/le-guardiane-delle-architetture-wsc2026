// Payment Bounded Context: modulo applicativo con wiring manuale.

import Database from 'better-sqlite3';
import { ApplicationModule, type WebApi } from '@/common/module/applicationModule.js';
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
import {
  paymentAcceptedIntegrationEvent,
  paymentExpiredIntegrationEvent,
  paymentRejectedIntegrationEvent,
  type PaymentResultIntegrationEvent,
} from './integration/paymentResultIntegrationEvent.js';
import type { PaymentRequestIntegrationCommand } from './integration/paymentRequestIntegrationCommand.js';
import type { RefundRequestIntegrationCommand } from './integration/refundRequestIntegrationCommand.js';
import { requestPayment } from './application/commands/requestPayment.js';
import { refundTransaction } from './application/commands/refundTransaction.js';
import { ClientReference } from '@/common/domain/primitive/clientReference.js';
import { Timestamp } from '@/common/domain/primitive/timestamp.js';
import { PaymentId } from './domain/payment/paymentId.js';
import { Uuid } from '@/common/domain/primitive/uuid.js';
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

type PaymentResultIntegrationHandler = (event: PaymentResultIntegrationEvent) => void;

export class PaymentModule extends ApplicationModule {
  private readonly _paymentRepository: PaymentRepository;
  private readonly _eventBus: InMemoryPaymentEventBus;
  private readonly _paymentRequesting: PaymentRequesting;
  private readonly _refundRequesting: RefundRequesting;
  private readonly _paymentProcessing: PaymentProcessing;
  private readonly _watcher: PaymentDeadlineWatcher;
  private readonly _paymentResultIntegrationHandlers: PaymentResultIntegrationHandler[];

  constructor(database: Database.Database) {
    super();

    requireDependency(database, "database");

    this._paymentResultIntegrationHandlers = [];
    this._paymentRepository = new SqlitePaymentRepository(database);
    this._eventBus = new InMemoryPaymentEventBus((task) => setImmediate(task));
    this._paymentRequesting = new PaymentRequesting(this._paymentRepository, this._eventBus);
    this._refundRequesting = new RefundRequesting(this._paymentRepository, this._eventBus);
    this._paymentProcessing = this.createPaymentProcessing();
    this._watcher = this.createWatcher();
    this.registerCrossBcResultHandlers();
  }

  private registerCrossBcResultHandlers(): void {
    this._eventBus.subscribe('PaymentAccepted', {
      on: (event) => {
        if (event.kind === 'PaymentAccepted') {
          const integrationEvent = paymentAcceptedIntegrationEvent(
            event.clientReference.value.value,
            event.amount
          );
          this._paymentResultIntegrationHandlers.forEach((handler) => handler(integrationEvent));
        }
      },
    });
    this._eventBus.subscribe('PaymentRejected', {
      on: (event) => {
        if (event.kind === 'PaymentRejected') {
          const integrationEvent = paymentRejectedIntegrationEvent(
            event.clientReference.value.value,
            event.amount,
            event.reason.value
          );
          this._paymentResultIntegrationHandlers.forEach((handler) => handler(integrationEvent));
        }
      },
    });
    this._eventBus.subscribe('PaymentExpired', {
      on: (event) => {
        if (event.kind === 'PaymentExpired') {
          const integrationEvent = paymentExpiredIntegrationEvent(
            event.clientReference.value.value,
            event.amount
          );
          this._paymentResultIntegrationHandlers.forEach((handler) => handler(integrationEvent));
        }
      },
    });
  }

  private createPaymentProcessing(): PaymentProcessing {
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

    return new PaymentProcessing(this._paymentRepository, providers, this._eventBus);
  }

  private createWatcher(): PaymentDeadlineWatcher {
    const paymentExpiration = new PaymentExpiration();
    return new PaymentDeadlineWatcher(this._paymentRepository, paymentExpiration, this._eventBus);
  }

  onPaymentResult(handler: PaymentResultIntegrationHandler): void {
    requireDependency(handler, 'handler');
    this._paymentResultIntegrationHandlers.push(handler);
  }

  requestPayment(command: PaymentRequestIntegrationCommand): void {
    requireArgument(command, 'command');
    const internalCommand = requestPayment(
      new PaymentId(Uuid.generate()),
      new ClientReference(Uuid.fromString(command.clientReference)),
      command.amount,
      Timestamp.now()
    );
    this._paymentRequesting.invoke(internalCommand);
  }

  requestRefund(command: RefundRequestIntegrationCommand): void {
    requireArgument(command, 'command');
    const clientReference = new ClientReference(Uuid.fromString(command.clientReference));
    const payment = this._paymentRepository.findByClientReference(clientReference);
    if (!payment) {
      throw new Error(`No payment found for clientReference: ${command.clientReference}`);
    }
    const internalCommand = refundTransaction(payment.id(), command.amount);
    this._refundRequesting.invoke(internalCommand);
  }

  webApis(): WebApi[] {
    const paymentFinder = new PaymentFinder(this._paymentRepository);
    return [
      new PaymentApi(paymentFinder, this._paymentProcessing),
      new PaymentInternalApi(this._paymentRequesting, paymentFinder),
    ];
  }

  start(): void {
    this._watcher.start();
  }

  stop(): void {
    this._watcher.stop();
  }
}
