// DTO di richiesta per la creazione interna di un Payment.

export type CreatePaymentRequest = {
  readonly paymentId: string;
  readonly clientReference: string;
  readonly amount: number | string;
  readonly requestedAt: string;
};

export function parseCreatePaymentRequest(body: unknown): CreatePaymentRequest {
  if (body === null || body === undefined || typeof body !== 'object') {
    throw new Error('request body is required');
  }

  const candidate = body as Record<string, unknown>;
  if (
    candidate.paymentId === null ||
    candidate.paymentId === undefined ||
    typeof candidate.paymentId !== 'string' ||
    candidate.paymentId.trim().length === 0
  ) {
    throw new Error('paymentId is required');
  }
  if (
    candidate.clientReference === null ||
    candidate.clientReference === undefined ||
    typeof candidate.clientReference !== 'string' ||
    candidate.clientReference.trim().length === 0
  ) {
    throw new Error('clientReference is required');
  }
  if (candidate.amount === null || candidate.amount === undefined) {
    throw new Error('amount is required');
  }
  if (
    candidate.requestedAt === null ||
    candidate.requestedAt === undefined ||
    typeof candidate.requestedAt !== 'string' ||
    candidate.requestedAt.trim().length === 0
  ) {
    throw new Error('requestedAt is required');
  }

  return {
    paymentId: candidate.paymentId,
    clientReference: candidate.clientReference,
    amount: typeof candidate.amount === 'number' ? candidate.amount : String(candidate.amount),
    requestedAt: candidate.requestedAt,
  };
}
