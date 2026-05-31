package it.giannibombelli.wsc2026.payment.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static it.giannibombelli.wsc2026.architecture.DomainApplicationPurityRules.noPrimitiveOrWrapperFields;
import static it.giannibombelli.wsc2026.architecture.DomainApplicationPurityRules.noPrimitiveOrWrapperParameters;
import static it.giannibombelli.wsc2026.architecture.DomainApplicationPurityRules.onlyProjectInternalsAndJdk;

@AnalyzeClasses(packages = "it.giannibombelli.wsc2026.payment", importOptions = ImportOption.DoNotIncludeTests.class)
class PaymentDomainApplicationPurityTest {

    private static final String BC = "it.giannibombelli.wsc2026.payment";

    @ArchTest
    static final ArchRule onlyProjectInternalsAndJdkRule = onlyProjectInternalsAndJdk(BC);

    @ArchTest
    static final ArchRule noPrimitiveOrWrapperFieldsRule = noPrimitiveOrWrapperFields(BC);

    @ArchTest
    static final ArchRule noPrimitiveOrWrapperParametersRule = noPrimitiveOrWrapperParameters(BC);
}
