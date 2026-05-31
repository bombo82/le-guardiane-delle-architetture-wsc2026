// Architectural Fitness Function: dipendenze tra Bounded Context.

import { describe, it, vi } from 'vitest';
import { projectFiles } from 'archunit';

vi.setConfig({ testTimeout: 20000 });

describe('CrossBoundedContextDependencies', () => {
  it('booking must not depend on other bounded contexts', async () => {
    const rule = projectFiles()
      .inFolder('src/booking/**')
      .shouldNot()
      .dependOnFiles()
      .inFolder(/src\/(giftcard|payment)/);

    await expect(rule).toPassAsync();
  });

  it('giftCard must not depend on other bounded contexts', async () => {
    const rule = projectFiles()
      .inFolder('src/giftcard/**')
      .shouldNot()
      .dependOnFiles()
      .inFolder(/src\/(booking|payment)/);

    await expect(rule).toPassAsync();
  });

  it('payment must not depend on other bounded contexts', async () => {
    const rule = projectFiles()
      .inFolder('src/payment/**')
      .shouldNot()
      .dependOnFiles()
      .inFolder(/src\/(booking|giftcard)/);

    await expect(rule).toPassAsync();
  });

  it('common must not depend on bounded contexts', async () => {
    const rule = projectFiles()
      .inFolder('src/common/**')
      .shouldNot()
      .dependOnFiles()
      .inFolder(/src\/(booking|giftcard|payment)/);

    await expect(rule).toPassAsync();
  });
});
