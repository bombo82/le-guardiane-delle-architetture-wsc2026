package it.giannibombelli.wsc2026.booking.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static it.giannibombelli.wsc2026.architecture.BoundedContextShapeRules.commandsMustImplementCommand;
import static it.giannibombelli.wsc2026.architecture.BoundedContextShapeRules.policiesMustImplementPolicy;

@AnalyzeClasses(packages = "it.giannibombelli.wsc2026.booking", importOptions = ImportOption.DoNotIncludeTests.class)
class BookingShapeRulesTest {

    private static final String BC = "it.giannibombelli.wsc2026.booking";

    @ArchTest
    static final ArchRule commandsMustImplementCommandRule = commandsMustImplementCommand(BC);

    @ArchTest
    static final ArchRule policiesMustImplementPolicyRule = policiesMustImplementPolicy(BC);
}
