package it.giannibombelli.wsc2026.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static it.giannibombelli.wsc2026.architecture.PublishedLanguageArchitectureRules.*;

/**
 * Verifica le regole di Published Language + Anti-Corruption Layer per le relazioni cross-BC attive.
 */
@AnalyzeClasses(packages = "it.giannibombelli.wsc2026", importOptions = ImportOption.DoNotIncludeTests.class)
class PublishedLanguageArchitectureTest {

    private static final String BOOKING_PACKAGE = "it.giannibombelli.wsc2026.booking";
    private static final String GIFTCARD_PACKAGE = "it.giannibombelli.wsc2026.giftcard";
    private static final String PAYMENT_PACKAGE = "it.giannibombelli.wsc2026.payment";

    @ArchTest
    static final ArchRule bookingPublishedLanguageMustBeIndependent =
        publishedLanguageMustNotDependOnInternalLayers(BOOKING_PACKAGE);

    @ArchTest
    static final ArchRule bookingPublishedLanguageMustNotDependOnGiftCard =
        publishedLanguageMustNotDependOnDownstream(BOOKING_PACKAGE, GIFTCARD_PACKAGE);

    @ArchTest
    static final ArchRule onlyGiftCardAclMayConsumeBookingPublishedLanguage =
        onlyAntiCorruptionLayerMayConsumePublishedLanguage(GIFTCARD_PACKAGE, "booking", BOOKING_PACKAGE);

    @ArchTest
    static final ArchRule paymentPublishedLanguageMustBeIndependent =
        publishedLanguageMustNotDependOnInternalLayers(PAYMENT_PACKAGE);

    @ArchTest
    static final ArchRule paymentPublishedLanguageMustNotDependOnBooking =
        publishedLanguageMustNotDependOnDownstream(PAYMENT_PACKAGE, BOOKING_PACKAGE);

    @ArchTest
    static final ArchRule paymentPublishedLanguageMustNotDependOnGiftCard =
        publishedLanguageMustNotDependOnDownstream(PAYMENT_PACKAGE, GIFTCARD_PACKAGE);

    @ArchTest
    static final ArchRule onlyBookingAclMayConsumePaymentPublishedLanguage =
        onlyAntiCorruptionLayerMayConsumePublishedLanguage(BOOKING_PACKAGE, "payment", PAYMENT_PACKAGE);

    @ArchTest
    static final ArchRule onlyGiftCardAclMayConsumePaymentPublishedLanguage =
        onlyAntiCorruptionLayerMayConsumePublishedLanguage(GIFTCARD_PACKAGE, "payment", PAYMENT_PACKAGE);
}
