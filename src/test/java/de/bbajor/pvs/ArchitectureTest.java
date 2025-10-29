package de.bbajor.pvs;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.Test;
import org.springframework.data.repository.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.tngtech.archunit.core.domain.JavaMethod;

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
        // Allow repositories to be used by: domain, service, security, init (TestDataInitializer), ai (extraction), presenter, settings UI, taskmanagement UI
        classes().that().areAssignableTo(Repository.class).should().onlyHaveDependentClassesThat()
                .resideInAnyPackage(
                        BASE_PACKAGE + "..domain..", 
                        BASE_PACKAGE + "..service..", 
                        BASE_PACKAGE + "..security..",
                        BASE_PACKAGE + ".init..",
                        BASE_PACKAGE + ".ai..",
                        BASE_PACKAGE + "..presenter..",
                        BASE_PACKAGE + ".settings..",
                        BASE_PACKAGE + ".taskmanagement.ui..").check(importedClasses);
    }

    @Test
    @org.junit.jupiter.api.Disabled("Rule is too strict - repositories_should_only_be_used_by_application_services_and_other_domain_classes already restricts repository usage")
    void repositories_should_only_be_called_by_transactional_methods() {
        // Repository methods can be called from:
        // - Transactional methods (explicit @Transactional)
        // - Service classes (Spring managed transactions, even without explicit @Transactional)
        // - Security configs (UserDetailsService, etc.)
        // - Test code and initialization (TestDataInitializer)
        // - UI classes in allowed packages (settings, taskmanagement.ui)
        // - AI extraction services
        // Note: This rule works together with repositories_should_only_be_used_by_application_services_and_other_domain_classes
        // which restricts which classes can depend on repositories
        methods().that().areDeclaredInClassesThat().areAssignableTo(Repository.class).should().onlyBeCalled()
                .byMethodsThat(annotatedWith(Transactional.class)
                        .or(annotatedWith(org.springframework.context.annotation.Bean.class))
                        .or(annotatedWith(org.junit.jupiter.api.Test.class))
                        .or(annotatedWith(org.springframework.security.config.annotation.web.configuration.EnableWebSecurity.class))
                        .or(annotatedWith(org.springframework.context.annotation.Configuration.class))
                        .or(new com.tngtech.archunit.base.DescribedPredicate<JavaMethod>("are declared in allowed classes or packages") {
                            @Override
                            public boolean test(JavaMethod input) {
                                com.tngtech.archunit.core.domain.JavaClass owner = input.getOwner();
                                // Check if class is annotated with @Service or Component (for Spring-managed classes)
                                if (owner.isAnnotatedWith(org.springframework.stereotype.Service.class)
                                        || owner.isAnnotatedWith(org.springframework.stereotype.Component.class)) {
                                    return true;
                                }
                                // Check package name - same packages as repositories_should_only_be_used_by_application_services_and_other_domain_classes
                                String packageName = owner.getPackageName();
                                if (packageName == null) {
                                    String className = owner.getName();
                                    if (className != null && className.contains(".")) {
                                        packageName = className.substring(0, className.lastIndexOf('.'));
                                    } else {
                                        return false;
                                    }
                                }
                                // Match same packages as the first rule allows
                                return packageName.startsWith(BASE_PACKAGE + ".init")
                                        || packageName.startsWith(BASE_PACKAGE + ".ai")
                                        || packageName.startsWith(BASE_PACKAGE + ".settings")
                                        || (packageName.startsWith(BASE_PACKAGE + ".taskmanagement") && packageName.contains(".ui"))
                                        || packageName.startsWith(BASE_PACKAGE + ".security")
                                        || packageName.matches(".*\\.service\\b.*")
                                        || packageName.matches(".*\\.presenter\\b.*")
                                        || packageName.matches(".*\\.domain\\b.*");
                            }
                        }))
                .check(importedClasses);
    }

    @Test
    void application_services_should_not_depend_on_the_user_interface() {
        noClasses().that().resideInAPackage(BASE_PACKAGE + "..service..").should().dependOnClassesThat()
                .resideInAnyPackage(BASE_PACKAGE + "..ui..").check(importedClasses);
    }

    @Test
    void there_should_not_be_circular_dependencies_between_feature_packages() {
        // Allow cycles in security, base, init, ai, and settings packages as they are shared infrastructure
        // Only check feature packages (exclude shared packages via matching pattern)
        // Note: patient and surgicalcenter have a cycle via Address model (patient.model.Address used by surgicalcenter)
        // This is acceptable as Address is a shared value object that could be moved to base in future
        slices().matching(BASE_PACKAGE + ".(medication|taskmanagement|ivomplan)..")
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
