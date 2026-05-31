package it.giannibombelli.wsc2026.payment.domain.payment;

import it.giannibombelli.wsc2026.common.utils.Require;

public enum Provider {
    PAYPAL("PayPal"),
    KLARNA("Klarna"),
    GIFT_CARD("GiftCard");

    private final String label;

    Provider(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public static Provider fromLabel(String label) {
        Require.requireArgument(label, "provider label");
        for (Provider provider : values()) {
            if (provider.label.equals(label)) {
                return provider;
            }
        }
        throw new IllegalArgumentException("unknown provider: " + label);
    }
}
