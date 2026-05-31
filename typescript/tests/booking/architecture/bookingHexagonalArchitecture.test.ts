// Architectural Fitness Function: layer interni del Booking Bounded Context.

import { describe, it, vi } from 'vitest';
import {
  apiMustNotDependOnInfrastructure,
  applicationMustNotDependOnAdapters,
  domainMustNotDependOnOuterLayers,
  infrastructureMustNotDependOnApi,
} from '../../architecture/boundedContextArchitectureRules.js';

vi.setConfig({ testTimeout: 20000 });

const BC = 'booking';

describe('BookingHexagonalArchitecture', () => {
  it('domain must not depend on outer layers', async () => {
    await expect(domainMustNotDependOnOuterLayers(BC)).toPassAsync();
  });

  it('application must not depend on adapters', async () => {
    await expect(applicationMustNotDependOnAdapters(BC)).toPassAsync();
  });

  it('infrastructure must not depend on api', async () => {
    await expect(infrastructureMustNotDependOnApi(BC)).toPassAsync();
  });

  it('api must not depend on infrastructure', async () => {
    await expect(apiMustNotDependOnInfrastructure(BC)).toPassAsync();
  });
});
