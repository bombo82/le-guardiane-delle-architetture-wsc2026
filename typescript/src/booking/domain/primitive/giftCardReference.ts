// Riferimento opaco a una gift card gestita dal BC `giftcard`.
// Il BC `booking` non conosce la struttura interna di `giftcard.GiftCardId`:
// utilizza solo questo value object per indicare a quale gift card si riferisce una prenotazione.

import { requireArgument } from '@/common/utils/requireArgument.js';
import { Uuid } from '@/common/domain/primitive/uuid.js';

export class GiftCardReference {
    readonly value: Uuid;

    constructor(value: Uuid) {
        requireArgument(value, 'value');
        this.value = value;
    }
}
