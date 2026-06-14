// Evento di dominio base per il GiftCard Bounded Context.

import type { GiftCardIssued } from './giftCardIssued.js';
import type { GiftCardTopUpRequested } from './giftCardTopUpRequested.js';
import type { TopUpConfirmed } from './topUpConfirmed.js';
import type { GiftCardCredited } from './giftCardCredited.js';
import type { GiftCardRefunded } from './giftCardRefunded.js';
import type { GiftCardRedeemEvent } from './giftCardRedeemEvents.js';

export type GiftCardEvent =
  | GiftCardIssued
  | GiftCardTopUpRequested
  | TopUpConfirmed
  | GiftCardCredited
  | GiftCardRefunded
  | GiftCardRedeemEvent;
