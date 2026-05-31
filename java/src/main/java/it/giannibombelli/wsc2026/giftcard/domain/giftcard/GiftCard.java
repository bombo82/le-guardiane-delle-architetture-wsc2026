package it.giannibombelli.wsc2026.giftcard.domain.giftcard;

import it.giannibombelli.wsc2026.common.utils.Require;

import it.giannibombelli.wsc2026.common.domain.model.Aggregate;
import it.giannibombelli.wsc2026.common.domain.primitive.Description;
import it.giannibombelli.wsc2026.common.domain.primitive.Money;
import it.giannibombelli.wsc2026.giftcard.domain.events.*;

import java.util.Objects;

public final class GiftCard implements Aggregate<GiftCardId> {
    private final GiftCardId id;
    private Money balance;

    public GiftCard(GiftCardId id, Money balance) {
        Require.requireArgument(id, "id");
        Require.requireArgument(balance, "balance");
        this.id = id;
        this.balance = balance;
    }

    public GiftCardId id() {
        return id;
    }

    public Money balance() {
        return balance;
    }

    public static GiftCard issue(GiftCardId id) {
        Require.requireArgument(id, "id");
        return new GiftCard(id, Money.zero());
    }

    public GiftCardTopUpRequested requestTopUp(Money amount) {
        Require.requireArgument(amount, "amount");
        return new GiftCardTopUpRequested(this.id, amount, this.balance);
    }

    public GiftCardRedeemEvents redeem(Money amount) {
        Require.requireArgument(amount, "amount");
        if (this.balance.isLessThan(amount)) {
            return new GiftCardRedeemEvents.GiftCardNotRedeemed(this.id, amount,
                new Description("insufficient balance"));
        }
        this.balance = this.balance.minus(amount);
        return new GiftCardRedeemEvents.GiftCardRedeemed(this.id, amount);
    }

    public GiftCardRefunded refund(Money amount) {
        Require.requireArgument(amount, "amount");
        this.balance = this.balance.plus(amount);
        return new GiftCardRefunded(this.id, amount);
    }

    public TopUpConfirmed confirmTopUp(Money amount) {
        Require.requireArgument(amount, "amount");
        this.balance = this.balance.plus(amount);
        return new TopUpConfirmed(this.id, amount);
    }

    public GiftCardCredited credit(Money amount) {
        Require.requireArgument(amount, "amount");
        this.balance = this.balance.plus(amount);
        return new GiftCardCredited(this.id, amount);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        return Objects.equals(id, ((GiftCard) o).id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "GiftCard[" +
            "id=" + id + ", " +
            "balance=" + balance +
            ']';
    }
}
