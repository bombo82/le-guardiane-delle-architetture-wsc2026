// Architectural Fitness Function: shape dei costrutti principali (Command, Policy)
// per il GiftCard Bounded Context.

import { describe, it, vi } from 'vitest';
import {
    commandsMustImplementCommand,
    policiesMustImplementPolicy,
} from '../../architecture/boundedContextShapeRules.js';

vi.setConfig({ testTimeout: 20000 });

const BC = 'giftcard';

describe('GiftCardShapeRules', () => {
    it('commands must implement Command', async () => {
        await expect(commandsMustImplementCommand(BC)).toPassAsync();
    });

    it('policies must implement Policy', async () => {
        await expect(policiesMustImplementPolicy(BC)).toPassAsync();
    });
});
