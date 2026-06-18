// Architectural Fitness Function: layer interni del Common Bounded Context.

import { describe, it, vi } from 'vitest';
import { projectFiles } from 'archunit';

vi.setConfig({ testTimeout: 20000 });

describe('CommonHexagonalArchitecture', () => {
  it('application should not depend on module', async () => {
    const rule = projectFiles()
      .inFolder('src/common/application/**')
      .shouldNot()
      .dependOnFiles()
      .inFolder('src/common/module*');

    await rule.check({ allowEmptyTests: true });
  });
});
