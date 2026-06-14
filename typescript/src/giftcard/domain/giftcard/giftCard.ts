// Aggregato GiftCard: emette e gestisce il saldo di una gift card.

import { Description } from '@/common/domain/primitive/description.js';
import { Money } from '@/common/domain/primitive/money.js';
import { GiftCardCredited, giftCardCredited } from '../events/giftCardCredited.js';
import type { GiftCardRedeemEvent } from '../events/giftCardRedeemEvents.js';
import { giftCardRedeemed, giftCardNotRedeemed } from '../events/giftCardRedeemEvents.js';
import { GiftCardRefunded, giftCardRefunded } from '../events/giftCardRefunded.js';
import { GiftCardTopUpRequested, giftCardTopUpRequested } from '../events/giftCardTopUpRequested.js';
import { TopUpConfirmed, topUpConfirmed } from '../events/topUpConfirmed.js';
import { GiftCardId } from './giftCardId.js';
import { requireArgument } from '@/common/utils/requireArgument.js';

export class GiftCard {
  private readonly _id: GiftCardId;
  private _balance: Money;

  constructor(id: GiftCardId, balance: Money) {
    requireArgument(id, 'id');
    requireArgument(balance, 'balance');
    this._id = id;
    this._balance = balance;
  }

  id(): GiftCardId {
    return this._id;
  }

  balance(): Money {
    return this._balance;
  }

  static issue(id: GiftCardId): GiftCard {
    requireArgument(id, 'id');
    return new GiftCard(id, Money.zero());
  }

  requestTopUp(amount: Money): GiftCardTopUpRequested {
    requireArgument(amount, 'amount');
    return giftCardTopUpRequested(this._id, amount, this._balance);
  }

  redeem(amount: Money): GiftCardRedeemEvent {
    requireArgument(amount, 'amount');
    if (this._balance.isLessThan(amount)) {
      return giftCardNotRedeemed(this._id, amount, new Description('insufficient balance'));
    }
    this._balance = this._balance.minus(amount);
    return giftCardRedeemed(this._id, amount);
  }

  refund(amount: Money): GiftCardRefunded {
    requireArgument(amount, 'amount');
    this._balance = this._balance.plus(amount);
    return giftCardRefunded(this._id, amount);
  }

  confirmTopUp(amount: Money): TopUpConfirmed {
    requireArgument(amount, 'amount');
    this._balance = this._balance.plus(amount);
    return topUpConfirmed(this._id, amount);
  }

  credit(amount: Money): GiftCardCredited {
    requireArgument(amount, 'amount');
    this._balance = this._balance.plus(amount);
    return giftCardCredited(this._id, amount);
  }
}
