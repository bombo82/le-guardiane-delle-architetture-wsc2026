import type { EntityId } from '../identity/entityId.js';

export interface Entity<ID extends EntityId> {
  id(): ID;
}
