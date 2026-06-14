// DTO di richiesta per l'inserimento di una prenotazione.

export type PlaceBookingRequest = {
  readonly amount: number;
  readonly description: string;
  readonly giftCardId: string;
};

function parseAmount(value: unknown): number {
  if (value === null || value === undefined) {
    throw new Error('amount is required');
  }
  if (typeof value === 'number') {
    return value;
  }
  if (typeof value === 'string' && value.trim().length > 0) {
    return Number(value);
  }
  throw new Error('amount must be a number');
}

export function parsePlaceBookingRequest(body: unknown): PlaceBookingRequest {
  if (body === null || body === undefined || typeof body !== 'object') {
    throw new Error('request body is required');
  }

  const candidate = body as Record<string, unknown>;
  const amount = parseAmount(candidate.amount);
  if (
    candidate.description === null ||
    candidate.description === undefined ||
    typeof candidate.description !== 'string' ||
    candidate.description.trim().length === 0
  ) {
    throw new Error('description is required');
  }
  if (candidate.giftCardId === null || candidate.giftCardId === undefined || typeof candidate.giftCardId !== 'string') {
    throw new Error('giftCardId is required');
  }

  return {
    amount,
    description: candidate.description,
    giftCardId: candidate.giftCardId,
  };
}
