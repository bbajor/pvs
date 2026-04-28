package de.bbajor.pvs;

import java.time.Clock;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(exclude = {
    // Exclude ALL Gateway auto-configurations (incompatible with Vaadin Flow/Spring MVC)
    // Gateway will be enabled when UI is migrated to Hilla (React) or runs as separate service
    org.springframework.cloud.gateway.config.GatewayAutoConfiguration.class,
    org.springframework.cloud.gateway.config.GatewayClassPathWarningAutoConfiguration.class,
    org.springframework.cloud.gateway.config.GatewayMetricsAutoConfiguration.class,
    org.springframework.cloud.gateway.config.GatewayReactiveLoadBalancerClientAutoConfiguration.class,
    org.springframework.cloud.gateway.config.GatewayNoLoadBalancerClientAutoConfiguration.class
})
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
        "de.bbajor.pvs.security.email.repository",
        "de.bbajor.pvs.ai.repository",
        "de.bbajor.pvs.analytics.repository"
})
@EntityScan(basePackages = {
        "de.bbajor.pvs.patient.model",
        "de.bbajor.pvs.medication.model",
        "de.bbajor.pvs.intravitreal.treatment.model",
        "de.bbajor.pvs.surgicalcenter.model",
        "de.bbajor.pvs.security.domain",
        "de.bbajor.pvs.security.email.model",
        "de.bbajor.pvs.base.domain",
        "de.bbajor.pvs.institution.model",
        "de.bbajor.pvs.location.model",
        "de.bbajor.pvs.appointment.model",
        "de.bbajor.pvs.taskmanagement.domain",
        "de.bbajor.pvs.ai.domain"
})
public class Application {

    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
    
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

}
