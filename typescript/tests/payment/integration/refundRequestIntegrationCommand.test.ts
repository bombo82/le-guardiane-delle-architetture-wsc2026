import { describe, expect, it } from 'vitest';
import { Money } from '@/common/domain/primitive/money.js';
import { refundRequestIntegrationCommand } from '@/payment/integration/refundRequestIntegrationCommand.js';

describe('RefundRequestIntegrationCommand', () => {
  it('creates a command with valid values', () => {
    const clientReference = crypto.randomUUID();
    const amount = new Money(50);

    const command = refundRequestIntegrationCommand(clientReference, amount);

    expect(command.clientReference).toBe(clientReference);
    expect(command.amount).toBe(amount);
  });

  it('throws on null clientReference', () => {
    expect(() => refundRequestIntegrationCommand(null as unknown as string, new Money(50))).toThrow();
  });

  it('throws on null amount', () => {
    expect(() => refundRequestIntegrationCommand(crypto.randomUUID(), null as unknown as Money)).toThrow();
  });
});
