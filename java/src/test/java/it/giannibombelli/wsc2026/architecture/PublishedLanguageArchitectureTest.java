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

    @ArchTest
    static final ArchRule bookingPublishedLanguageMustBeIndependent =
        publishedLanguageMustNotDependOnInternalLayers(BOOKING_PACKAGE);

    @ArchTest
    static final ArchRule bookingPublishedLanguageMustNotDependOnGiftCard =
        publishedLanguageMustNotDependOnDownstream(BOOKING_PACKAGE, GIFTCARD_PACKAGE);

    @ArchTest
    static final ArchRule onlyGiftCardAclMayConsumeBookingPublishedLanguage =
        onlyAntiCorruptionLayerMayConsumePublishedLanguage(GIFTCARD_PACKAGE, "booking", BOOKING_PACKAGE);
}
