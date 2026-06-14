// DTO di richiesta per una ricarica.

export type RequestTopUpRequest = {
  readonly amount: number;
};

export function parseRequestTopUpRequest(body: unknown): RequestTopUpRequest {
  if (body === null || body === undefined || typeof body !== 'object') {
    throw new Error('request body is required');
  }

  const candidate = body as Record<string, unknown>;
  if (candidate.amount === null || candidate.amount === undefined) {
    throw new Error('amount is required');
  }
  if (typeof candidate.amount !== 'number') {
    throw new Error('amount must be a number');
  }

  return { amount: candidate.amount };
}
