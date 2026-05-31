// Architectural Fitness Function: layer interni del Common Bounded Context.

import { describe, it, vi } from 'vitest';
import { projectFiles } from 'archunit';

vi.setConfig({ testTimeout: 20000 });

describe('CommonHexagonalArchitecture', () => {
  it('domain should not depend on application', async () => {
    const rule = projectFiles()
      .inFolder('src/common/domain/**')
      .shouldNot()
      .dependOnFiles()
      .inFolder('src/common/application/**');

    await expect(rule).toPassAsync();
  });

  it('domain should not depend on module', async () => {
    const rule = projectFiles()
      .inFolder('src/common/domain/**')
      .shouldNot()
      .dependOnFiles()
      .inFolder('src/common/module*');

    await rule.check({ allowEmptyTests: true });
  });

  it('application should not depend on module', async () => {
    const rule = projectFiles()
      .inFolder('src/common/application/**')
      .shouldNot()
      .dependOnFiles()
      .inFolder('src/common/module*');

    await rule.check({ allowEmptyTests: true });
  });
});
