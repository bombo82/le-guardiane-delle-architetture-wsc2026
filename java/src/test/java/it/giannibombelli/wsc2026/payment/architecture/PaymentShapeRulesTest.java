package it.giannibombelli.wsc2026.payment.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static it.giannibombelli.wsc2026.architecture.BoundedContextShapeRules.commandsMustImplementCommand;
import static it.giannibombelli.wsc2026.architecture.BoundedContextShapeRules.policiesMustImplementPolicy;

@AnalyzeClasses(packages = "it.giannibombelli.wsc2026.payment", importOptions = ImportOption.DoNotIncludeTests.class)
class PaymentShapeRulesTest {

    private static final String BC = "it.giannibombelli.wsc2026.payment";

    @ArchTest
    static final ArchRule commandsMustImplementCommandRule = commandsMustImplementCommand(BC);

    @ArchTest
    static final ArchRule policiesMustImplementPolicyRule = policiesMustImplementPolicy(BC);
}
