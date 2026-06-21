// Servizio applicativo che gestisce le richieste di rimborso contattando il provider.

import { Description } from '@/common/domain/primitive/description.js';
import { Money } from '@/common/domain/primitive/money.js';
import type { EventPublisher } from '@/common/application/events/eventPublisher.js';
import type { EventSubscriber } from '@/common/application/events/eventSubscriber.js';
import { PaymentEvent } from '../../domain/events/paymentEvent.js';
import { RefundRequested } from '../../domain/events/refundRequested.js';
import type {
  TransactionRefunded,
  TransactionNotRefunded,
} from '../../domain/events/refundResultEvents.js';
import { PaymentNotFoundException } from '../../domain/payment/paymentNotFoundException.js';
import { Transaction } from '../../domain/payment/transaction.js';
import { TransactionStatus } from '../../domain/payment/transactionStatus.js';
import { TransactionRefund } from '../../application/policies/transactionRefund.js';
import type { PaymentProvider } from '../../domain/ports/paymentProvider.js';
import { PaymentProviderResult } from '../../domain/ports/paymentProviderResult.js';
import type { PaymentRepository } from '../../domain/ports/paymentRepository.js';
import { requireArgument } from '@/common/utils/requireArgument.js';
import { requireDependency } from '@/common/utils/requireDependency.js';

export class RefundHandling implements EventSubscriber<RefundRequested> {
  private readonly _paymentRepository: PaymentRepository;
  private readonly _providers: Record<string, PaymentProvider>;
  private readonly _transactionRefund: TransactionRefund;
  private readonly _eventPublisher: EventPublisher<PaymentEvent>;

  constructor(
    paymentRepository: PaymentRepository,
    providers: Record<string, PaymentProvider>,
    transactionRefund: TransactionRefund,
    eventPublisher: EventPublisher<PaymentEvent>
  ) {
    requireDependency(paymentRepository, "paymentRepository");
    requireDependency(providers, "providers");
    requireDependency(transactionRefund, "transactionRefund");
    requireDependency(eventPublisher, "eventPublisher");
    this._paymentRepository = paymentRepository;
    this._providers = providers;
    this._transactionRefund = transactionRefund;
    this._eventPublisher = eventPublisher;
  }

  on(event: RefundRequested): void {
    requireArgument(event, 'event');

    const cmd = this._transactionRefund.evaluate(event);

    const payment = this._paymentRepository.findById(cmd.aggregateId);
    if (payment === null) throw new PaymentNotFoundException();

    const acceptedTransactions = payment.transactions().filter((t) => t.status() === TransactionStatus.ACCEPTED);
    if (acceptedTransactions.length === 0) {
      throw new Error('payment has no accepted transaction');
    }

    let refundedSum = Money.zero();
    let failedTransaction: Transaction | null = null;
    let failureReason: Description | null = null;

    for (const transaction of acceptedTransactions) {
      const provider = this._providers[transaction.provider()];
      if (provider === undefined) {
        failedTransaction = transaction;
        failureReason = new Description(`unknown provider: ${transaction.provider()}`);
        break;
      }

      const result: PaymentProviderResult = provider.refund(
        payment.id().value,
        transaction.providerReference()!.value,
        transaction.amount()
      );

      if (result.kind === 'success') {
        payment.markTransactionRefunded(transaction.id());
        refundedSum = refundedSum.plus(transaction.amount());
      } else {
        failedTransaction = transaction;
        failureReason = result.reason;
      }

      if (failedTransaction !== null) {
        break;
      }
    }

    if (failedTransaction !== null) {
      const notRefunded: TransactionNotRefunded = payment.rejectRefund(
        failedTransaction.provider(),
        failedTransaction.providerReference()!,
        failureReason!
      );
      this._paymentRepository.save(payment);
      this._eventPublisher.publish(notRefunded);
      return;
    }

    const refunded: TransactionRefunded = payment.refundTransaction(refundedSum);
    this._paymentRepository.save(payment);
    this._eventPublisher.publish(refunded);
  }
}
