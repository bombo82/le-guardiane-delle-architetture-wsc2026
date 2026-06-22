// Architectural Fitness Functions per proteggere il pattern Published Language + Anti-Corruption Layer.

import { describe, it, vi } from 'vitest';
import { projectFiles } from 'archunit';

vi.setConfig({ testTimeout: 20000 });

function internalLayersOf(bc: string): RegExp {
  return new RegExp(`src\\/${bc}\\/(?!integration\\/).*`);
}

describe('PublishedLanguageArchitecture', () => {
  it('booking PL must not depend on booking internal layers', async () => {
    const rule = projectFiles()
      .inFolder('src/booking/integration/**')
      .shouldNot()
      .dependOnFiles()
      .inFolder(internalLayersOf('booking'));

    await expect(rule).toPassAsync();
  });

  it('booking PL must not depend on giftcard', async () => {
    const rule = projectFiles()
      .inFolder('src/booking/integration/giftcard/**')
      .shouldNot()
      .dependOnFiles()
      .inFolder('src/giftcard/**');

    await expect(rule).toPassAsync();
  });

  it('only giftcard ACL may depend on booking PL', async () => {
    const rule = projectFiles()
      .inFolder(/src\/giftcard\/(?!application\/integration\/booking(\/|$)).*/)
      .shouldNot()
      .dependOnFiles()
      .inFolder('src/booking/integration/**');

    await expect(rule).toPassAsync();
  });

  it('payment PL must not depend on payment internal layers', async () => {
    const rule = projectFiles()
      .inFolder('src/payment/integration/**')
      .shouldNot()
      .dependOnFiles()
      .inFolder(/src\/payment\/(domain|application|api|infrastructure)\//);

    await expect(rule).toPassAsync();
  });

  it('payment PL must not depend on booking', async () => {
    const rule = projectFiles()
      .inFolder('src/payment/integration/**')
      .shouldNot()
      .dependOnFiles()
      .inFolder('src/booking/**');

    await expect(rule).toPassAsync();
  });

  it('payment PL must not depend on giftcard', async () => {
    const rule = projectFiles()
      .inFolder('src/payment/integration/**')
      .shouldNot()
      .dependOnFiles()
      .inFolder('src/giftcard/**');

    await expect(rule).toPassAsync();
  });

  it('only booking ACL may depend on payment PL', async () => {
    const rule = projectFiles()
      .inFolder(/src\/booking\/(?!application\/integration\/payment(\/|$)).*/)
      .shouldNot()
      .dependOnFiles()
      .inFolder('src/payment/integration/**');

    await expect(rule).toPassAsync();
  });

  it('only giftcard ACL may depend on payment PL', async () => {
    const rule = projectFiles()
      .inFolder(/src\/giftcard\/(?!application\/integration\/payment(\/|$)).*/)
      .shouldNot()
      .dependOnFiles()
      .inFolder('src/payment/integration/**');

    await expect(rule).toPassAsync();
  });
});
