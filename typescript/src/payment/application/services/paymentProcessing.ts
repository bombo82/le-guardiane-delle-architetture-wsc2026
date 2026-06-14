// Servizio applicativo che carica un Payment, avvia una transazione,
// invoca il provider e pubblica gli eventi di esito.

import { generateId } from '@/common/domain/identity/entityId.js';
import { Uuid } from '@/common/domain/primitive/uuid.js';
import type { UseCase } from '@/common/application/usecase.js';
import type { EventPublisher } from '@/common/application/events/eventPublisher.js';
import { StartTransaction } from '../commands/startTransaction.js';
import { PaymentEvent } from '../../domain/events/paymentEvent.js';
import { transactionAccepted } from '../../domain/events/transactionAccepted.js';
import { transactionRejected } from '../../domain/events/transactionRejected.js';
import type { TransactionStarted } from '../../domain/events/transactionStarted.js';
import { PaymentCharging } from '../../domain/policies/paymentCharging.js';
import { PaymentNotFoundException } from '../../domain/payment/paymentNotFoundException.js';
import { TransactionId } from '../../domain/payment/transactionId.js';
import type { PaymentProvider } from '../../domain/ports/paymentProvider.js';
import { PaymentProviderResult } from '../../domain/ports/paymentProviderResult.js';
import type { PaymentRepository } from '../../domain/ports/paymentRepository.js';
import { requireArgument } from '@/common/utils/requireArgument.js';
import { requireDependency } from '@/common/utils/requireDependency.js';

export class PaymentProcessing implements UseCase<StartTransaction, TransactionStarted> {
  private readonly _paymentRepository: PaymentRepository;
  private readonly _providers: Record<string, PaymentProvider>;
  private readonly _eventPublisher: EventPublisher<PaymentEvent>;

  constructor(
    paymentRepository: PaymentRepository,
    providers: Record<string, PaymentProvider>,
    eventPublisher: EventPublisher<PaymentEvent>
  ) {
    requireDependency(paymentRepository, "paymentRepository");
    requireDependency(providers, "providers");
    requireDependency(eventPublisher, "eventPublisher");
    this._paymentRepository = paymentRepository;
    this._providers = providers;
    this._eventPublisher = eventPublisher;
  }

  invoke(cmd: StartTransaction): TransactionStarted {
    requireArgument(cmd, 'command');

    const payment = this._paymentRepository.findById(cmd.aggregateId);
    if (payment === null) throw new PaymentNotFoundException();

    const provider = this._providers[cmd.provider];
    if (provider === undefined) {
      throw new Error(`unknown provider: ${cmd.provider}`);
    }

    if (!cmd.amount.equals(payment.amount())) {
      throw new Error('payment amount must be covered by a single transaction');
    }

    const transactionId = generateId((value: Uuid) => new TransactionId(value));
    const started = payment.startTransaction(
      transactionId,
      cmd.provider,
      cmd.providerReference,
      cmd.amount,
      cmd.startedAt
    );
    this._paymentRepository.save(payment);
    this._eventPublisher.publish(started);

    const charging = new PaymentCharging(provider);
    const result: PaymentProviderResult = charging.charge(started);

    if (result.kind === 'success') {
      this._eventPublisher.publish(
        transactionAccepted(
          payment.id(),
          cmd.provider,
          started.transactionId,
          cmd.amount,
          result.providerCompletedAt
        )
      );
    } else {
      this._eventPublisher.publish(
        transactionRejected(payment.id(), cmd.provider, started.transactionId, result.reason)
      );
    }

    return started;
  }
}
