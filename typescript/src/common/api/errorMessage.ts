// Helper condiviso per estrarre un messaggio di errore leggibile.

export function getErrorMessage(error: unknown, fallback = 'unexpected error'): string {
  return error instanceof Error ? error.message : fallback;
}
