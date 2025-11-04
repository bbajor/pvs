package de.bbajor.pvs;

import java.time.Clock;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.theme.Theme;

@SpringBootApplication
@EnableJpaRepositories(basePackages = {
        "de.bbajor.pvs.patient.repository",
        "de.bbajor.pvs.medication.repository",
        "de.bbajor.pvs.intravitreal.treatment.repository",
        "de.bbajor.pvs.surgicalcenter.repository",
        "de.bbajor.pvs.taskmanagement.domain",
        "de.bbajor.pvs.institution.repository",
        "de.bbajor.pvs.location.repository",
        "de.bbajor.pvs.appointment.repository",
        "de.bbajor.pvs.security.domain",
        "de.bbajor.pvs.ai.repository"
})
@EntityScan(basePackages = {
        "de.bbajor.pvs.patient.model",
        "de.bbajor.pvs.medication.model",
        "de.bbajor.pvs.intravitreal.treatment.model",
        "de.bbajor.pvs.surgicalcenter.model",
        "de.bbajor.pvs.security.domain",
        "de.bbajor.pvs.base.domain",
        "de.bbajor.pvs.institution.model",
        "de.bbajor.pvs.location.model",
        "de.bbajor.pvs.appointment.model",
        "de.bbajor.pvs.taskmanagement.domain",
        "de.bbajor.pvs.ai.domain"
})
@Theme("default")
public class Application implements AppShellConfigurator {

    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
    
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

}
