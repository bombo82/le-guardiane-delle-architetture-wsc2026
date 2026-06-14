// DTO di richiesta per l'avvio di una transazione.

export type StartTransactionRequest = {
  readonly provider: string;
  readonly providerReference: string | null;
  readonly amount: number | string;
};

export function parseStartTransactionRequest(body: unknown): StartTransactionRequest {
  if (body === null || body === undefined || typeof body !== 'object') {
    throw new Error('request body is required');
  }

  const candidate = body as Record<string, unknown>;
  if (
    candidate.provider === null ||
    candidate.provider === undefined ||
    typeof candidate.provider !== 'string' ||
    candidate.provider.trim().length === 0
  ) {
    throw new Error('provider is required');
  }
  if (candidate.amount === null || candidate.amount === undefined) {
    throw new Error('amount is required');
  }

  const providerReference = candidate.providerReference;
  return {
    provider: candidate.provider,
    providerReference: providerReference === null || providerReference === undefined ? null : String(providerReference),
    amount: typeof candidate.amount === 'number' ? candidate.amount : String(candidate.amount),
  };
}
