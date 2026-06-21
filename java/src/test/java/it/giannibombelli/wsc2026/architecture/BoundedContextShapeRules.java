package it.giannibombelli.wsc2026.architecture;

import com.tngtech.archunit.lang.ArchRule;
import it.giannibombelli.wsc2026.common.application.Command;
import it.giannibombelli.wsc2026.common.application.Policy;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

/**
 * Regole di shape condivise per la verifica dei costrutti principali
 * (Command e Policy) all'interno di un singolo Bounded Context.
 */
public final class BoundedContextShapeRules {

    private BoundedContextShapeRules() {
    }

    public static ArchRule commandsMustImplementCommand(String boundedContextPackage) {
        return classes()
            .that().resideInAPackage(boundedContextPackage + ".application.commands..")
            .and().areNotInterfaces()
            .should().implement(Command.class)
            .because("every concrete command in " + boundedContextPackage + " must implement Command");
    }

    public static ArchRule policiesMustImplementPolicy(String boundedContextPackage) {
        return classes()
            .that().resideInAPackage(boundedContextPackage + ".application.policies..")
            .and().areNotInterfaces()
            .should().implement(Policy.class)
            .because("every concrete policy in " + boundedContextPackage + " must implement Policy");
    }
}
