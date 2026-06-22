// Architectural Fitness Function: dipendenze tra Bounded Context.

import { describe, it, vi } from 'vitest';
import { projectFiles } from 'archunit';

vi.setConfig({ testTimeout: 20000 });

// Matcha tutto ciò che sta sotto src/<bc>/ per i BC indicati,
// escludendo il sotto-package integration che rappresenta la Published Language pubblica.
function otherBoundedContexts(bcs: string[]): RegExp {
  return new RegExp(`src\\/(${bcs.join('|')})\\/(?!integration(\\/|$)).*`);
}

describe('CrossBoundedContextDependencies', () => {
  it('booking must not depend on other bounded contexts', async () => {
    const rule = projectFiles()
      .inFolder('src/booking/**')
      .shouldNot()
      .dependOnFiles()
      .inFolder(otherBoundedContexts(['giftcard', 'payment']));

    await expect(rule).toPassAsync();
  }, 30000);

  it('giftcard must not depend on other bounded contexts', async () => {
    const rule = projectFiles()
      .inFolder('src/giftcard/**')
      .shouldNot()
      .dependOnFiles()
      .inFolder(otherBoundedContexts(['booking', 'payment']));

    await expect(rule).toPassAsync();
  });

  it('payment must not depend on other bounded contexts', async () => {
    const rule = projectFiles()
      .inFolder('src/payment/**')
      .shouldNot()
      .dependOnFiles()
      .inFolder(otherBoundedContexts(['booking', 'giftcard']));

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
