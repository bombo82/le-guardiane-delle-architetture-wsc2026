// Query service per recuperare i Payment in forma di read model.

import { ClientReference } from '@/common/domain/primitive/clientReference.js';
import { Payment } from '../../domain/payment/payment.js';
import { PaymentId } from '../../domain/payment/paymentId.js';
import { Transaction } from '../../domain/payment/transaction.js';
import type { PaymentRepository } from '../../domain/ports/paymentRepository.js';
import { PaymentDetails } from './paymentDetails.js';
import { PaymentSummary } from './paymentSummary.js';
import { requireArgument } from '@/common/utils/requireArgument.js';
import { requireDependency } from '@/common/utils/requireDependency.js';

export class PaymentFinder {
  private readonly _paymentRepository: PaymentRepository;

  constructor(paymentRepository: PaymentRepository) {
    requireDependency(paymentRepository, "paymentRepository");
    this._paymentRepository = paymentRepository;
  }

  findDetailsById(id: PaymentId): PaymentDetails | null {
    requireArgument(id, 'paymentId');

    const payment = this._paymentRepository.findById(id);
    return payment === null ? null : this.toDetails(payment);
  }

  findSummaryById(id: PaymentId): PaymentSummary | null {
    requireArgument(id, 'paymentId');

    const payment = this._paymentRepository.findById(id);
    return payment === null ? null : this.toSummary(payment);
  }

  findSummaryByClientReference(clientReference: ClientReference): PaymentSummary | null {
    requireArgument(clientReference, 'clientReference');

    const payment = this._paymentRepository.findByClientReference(clientReference);
    return payment === null ? null : this.toSummary(payment);
  }

  findDetailsByClientReference(clientReference: ClientReference): PaymentDetails | null {
    requireArgument(clientReference, 'clientReference');

    const payment = this._paymentRepository.findByClientReference(clientReference);
    return payment === null ? null : this.toDetails(payment);
  }

  private toDetails(payment: Payment): PaymentDetails {
    return {
      id: payment.id().value.value,
      clientReference: payment.clientReference(),
      amount: payment.amount(),
      status: payment.status(),
      requestedAt: payment.requestedAt(),
      transactions: payment.transactions().map((t) => this.toTransactionDetail(t)),
    };
  }

  private toSummary(payment: Payment): PaymentSummary {
    return {
      id: payment.id().value.value,
      clientReference: payment.clientReference(),
      amount: payment.amount(),
      status: payment.status(),
      requestedAt: payment.requestedAt(),
    };
  }

  private toTransactionDetail(transaction: Transaction): PaymentDetails.TransactionDetail {
    const providerReference = transaction.providerReference();
    const completedAt = transaction.completedAt();
    return {
      id: transaction.id().value.value,
      provider: transaction.provider(),
      providerReference: providerReference === null ? null : providerReference.value.value,
      amount: transaction.amount(),
      status: transaction.status(),
      startedAt: transaction.startedAt(),
      completedAt: completedAt === null ? null : completedAt,
    };
  }
}
