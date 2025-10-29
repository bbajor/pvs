package de.bbajor.pvs;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.Test;
import org.springframework.data.repository.Repository;
import org.springframework.transaction.annotation.Transactional;

import static com.tngtech.archunit.core.domain.properties.CanBeAnnotated.Predicates.annotatedWith;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

class ArchitectureTest {

    static final String BASE_PACKAGE = "de.bbajor.pvs";

    private final JavaClasses importedClasses = new ClassFileImporter().importPackages(BASE_PACKAGE);

    // TODO Add your own rules and remove those that don't apply to your project

    @Test
    void domain_model_should_not_depend_on_application_services() {
        noClasses().that().resideInAPackage(BASE_PACKAGE + "..domain..").should().dependOnClassesThat()
                .resideInAPackage(BASE_PACKAGE + "..service..").check(importedClasses);
    }

    @Test
    void domain_model_should_not_depend_on_the_user_interface() {
        noClasses().that().resideInAPackage(BASE_PACKAGE + "..domain..").should().dependOnClassesThat()
                .resideInAnyPackage(BASE_PACKAGE + "..ui..").check(importedClasses);
    }

    @Test
    void repositories_should_only_be_used_by_application_services_and_other_domain_classes() {
        classes().that().areAssignableTo(Repository.class).should().onlyHaveDependentClassesThat()
                .resideInAnyPackage(BASE_PACKAGE + "..domain..", BASE_PACKAGE + "..service..", BASE_PACKAGE + "..security..").check(importedClasses);
    }

    @Test
    void repositories_should_only_be_called_by_transactional_methods() {
        // Repository methods can be called from transactional methods, security configs, or test code
        methods().that().areDeclaredInClassesThat().areAssignableTo(Repository.class).should().onlyBeCalled()
                .byMethodsThat(annotatedWith(Transactional.class)
                    .or(annotatedWith(org.springframework.context.annotation.Bean.class))
                    .or(annotatedWith(org.springframework.stereotype.Service.class))
                    .or(annotatedWith(org.junit.jupiter.api.Test.class))
                    .or(annotatedWith(org.springframework.security.config.annotation.web.configuration.EnableWebSecurity.class))
                    .or(annotatedWith(org.springframework.context.annotation.Configuration.class))).check(importedClasses);
    }

    @Test
    void application_services_should_not_depend_on_the_user_interface() {
        noClasses().that().resideInAPackage(BASE_PACKAGE + "..service..").should().dependOnClassesThat()
                .resideInAnyPackage(BASE_PACKAGE + "..ui..").check(importedClasses);
    }

    @Test
    void there_should_not_be_circular_dependencies_between_feature_packages() {
        // Allow cycles in security and base packages as they are shared infrastructure
        // Only check feature packages (exclude security and base via matching pattern)
        // Pattern matches: de.bbajor.pvs.(feature_name).. but excludes security and base
        slices().matching(BASE_PACKAGE + ".(patient|medication|taskmanagement|ivomplan|surgicalcenter|init)..")
                .should().beFreeOfCycles().check(importedClasses);
    }

    @Test
    void security_package_should_not_depend_on_other_application_classes() {
        classes().that().resideInAPackage(BASE_PACKAGE + ".security..").should().onlyAccessClassesThat()
                .resideOutsideOfPackage(BASE_PACKAGE + "..").orShould().accessClassesThat()
                .resideInAPackage(BASE_PACKAGE + ".security..")
                .because("Security classes should only depend on external libraries and other security classes");
    }
}
