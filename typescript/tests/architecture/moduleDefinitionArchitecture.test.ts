// Architectural Fitness Functions: verifica che i moduli (facciata di ciascun Bounded Context)
// rispettino un contratto pubblico minimale e non espongano dettagli interni o dipendenze framework.

import { describe, it, vi } from 'vitest';
import { projectFiles } from 'archunit';

vi.setConfig({ testTimeout: 20000 });

describe('ModuleDefinitionArchitecture', () => {
  it('module private fields should be readonly to ensure immutable state after construction', async () => {
    const rule = projectFiles()
      .inPath(/src\/(booking|giftcard|payment)\/module\.ts$/)
      .should()
      .adhereTo(
        (fileInfo) => !/^  private _[a-zA-Z]\w*:/m.test(fileInfo.content),
        'module private fields should be readonly; non-readonly fields indicate lifecycle issues (e.g. watcher initialized in configure())'
      );

    await expect(rule).toPassAsync();
  });

  it('module facade must not depend on Express configuration types', async () => {
    const rule = projectFiles()
      .inPath(/src\/(booking|giftcard|payment)\/module\.ts$/)
      .should()
      .adhereTo(
        (fileInfo) => !/import\s+(?:type\s+)?\{[^}]*\bExpress\b[^}]*\}/.test(fileInfo.content),
        'module facade should not depend directly on Express configuration types; the framework should be kept at the infrastructure/api boundary'
      );

    await expect(rule).toPassAsync();
  });
});
