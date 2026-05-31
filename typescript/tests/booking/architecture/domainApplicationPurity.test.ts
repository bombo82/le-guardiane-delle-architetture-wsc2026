// Architectural Fitness Function: purezza dei layer domain/application
// per il Booking Bounded Context.

import { describe, it, vi } from 'vitest';
import {
    shouldNotDeclarePrimitiveFields,
    shouldNotUsePrimitiveParameters,
    shouldOnlyDependOnProjectInternals,
} from '../../architecture/domainApplicationPurityRules.js';

vi.setConfig({ testTimeout: 20000 });

const BC = 'booking';

describe('BookingDomainApplicationPurity', () => {
    it('domain and application should only depend on project internals and built-in modules', async () => {
        await expect(shouldOnlyDependOnProjectInternals(BC)).toPassAsync();
    });

    it('domain and application should not declare primitive/String/wrapper fields', async () => {
        await expect(shouldNotDeclarePrimitiveFields(BC)).toPassAsync();
    });

    it('domain and application should not use primitive/String/wrapper parameters', async () => {
        await expect(shouldNotUsePrimitiveParameters(BC)).toPassAsync();
    });
});
