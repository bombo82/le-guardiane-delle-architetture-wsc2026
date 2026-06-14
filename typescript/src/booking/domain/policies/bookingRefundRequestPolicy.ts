// Policy che traduce un BookingRefused in una richiesta di rimborso per il Payment BC.


import type { Policy } from '@/common/domain/model/policy.js';
import { ClientReference } from '@/common/domain/primitive/clientReference.js';
import { refundTransaction, type RefundTransaction } from '@/payment/application/commands/refundTransaction.js';
import type { PaymentRepository } from '@/payment/domain/ports/paymentRepository.js';
import { BookingRefused } from '../events/bookingResultEvents.js';
import { requireArgument } from '@/common/utils/requireArgument.js';
import { requireDependency } from '@/common/utils/requireDependency.js';

export class BookingRefundRequestPolicy implements Policy<BookingRefused, RefundTransaction> {
  private readonly _paymentRepository: PaymentRepository;

  constructor(paymentRepository: PaymentRepository) {
    requireDependency(paymentRepository, "paymentRepository");
    this._paymentRepository = paymentRepository;
  }

  evaluate(event: BookingRefused): RefundTransaction {
    requireArgument(event, 'Booking refused event');

    const clientReference = new ClientReference(event.aggregateId.value.value);
    const payment = this._paymentRepository.findByClientReference(clientReference);
    if (payment === null) {
      throw new Error(`Payment not found for booking: ${clientReference.value}`);
    }

    return refundTransaction(payment.id(), event.amount);
  }
}
