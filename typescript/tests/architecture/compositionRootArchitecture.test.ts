// Architectural Fitness Functions: il composition root non deve accedere agli internals dei moduli.

import { describe, it, vi } from 'vitest';
import { projectFiles } from 'archunit';

vi.setConfig({ testTimeout: 20000 });

describe('CompositionRootArchitecture', () => {
  it('application root must not access integration handlers of modules', async () => {
    const rule = projectFiles()
      .inPath(/src\/application\.ts$/)
      .should()
      .adhereTo(
        (fileInfo) =>
          !/handlePaymentResultFromPayment|confirmTopUpFromPayment|creditFromBooking|refundFromBooking/.test(
            fileInfo.content
          ),
        'application root must not access integration handlers exposed by modules'
      );

    await expect(rule).toPassAsync();
  });

  it('application root must not depend on integration adapters of modules', async () => {
    const rule = projectFiles()
      .inPath(/src\/application\.ts$/)
      .shouldNot()
      .dependOnFiles()
      .inPath(/src\/(booking|giftcard)\/application\/integration\/[^/]+\/adapter\/.*\.ts$/);

    await expect(rule).toPassAsync();
  });
});
