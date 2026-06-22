import { describe, expect, it } from 'vitest';
import { Money } from '@/common/domain/primitive/money.js';
import { paymentRequestIntegrationCommand } from '@/payment/integration/paymentRequestIntegrationCommand.js';

describe('PaymentRequestIntegrationCommand', () => {
  it('creates a command with valid values', () => {
    const clientReference = crypto.randomUUID();
    const amount = new Money(50);

    const command = paymentRequestIntegrationCommand(clientReference, amount);

    expect(command.clientReference).toBe(clientReference);
    expect(command.amount).toBe(amount);
  });

  it('throws on null clientReference', () => {
    expect(() => paymentRequestIntegrationCommand(null as unknown as string, new Money(50))).toThrow();
  });

  it('throws on null amount', () => {
    expect(() => paymentRequestIntegrationCommand(crypto.randomUUID(), null as unknown as Money)).toThrow();
  });
});
