package de.bbajor.pvs.function;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Spring Cloud Function Configuration.
 * 
 * This configuration imports all function configurations from individual service modules.
 * Functions are platform-agnostic and can be deployed to OpenFaaS, AWS Lambda,
 * Azure Functions, or any other FaaS platform.
 * 
 * Functions follow the pattern: Function<Request, Response>
 * - Request contains institutionId for multi-tenant isolation
 * - Response contains the result or error information
 */
@Configuration
@Import({
    de.bbajor.pvs.function.patient.PatientFunctions.class,
    de.bbajor.pvs.function.institution.InstitutionFunctions.class,
    de.bbajor.pvs.function.scheduled.DailyTaskFunction.class,
    de.bbajor.pvs.function.ai.AiFunctions.class,
    de.bbajor.pvs.function.egk.EgkFunctions.class,
    de.bbajor.pvs.function.treatment.TreatmentFunctions.class,
    de.bbajor.pvs.function.task.TaskFunctions.class,
    de.bbajor.pvs.function.analytics.AnalyticsFunctions.class,
    de.bbajor.pvs.function.appointment.AppointmentFunctions.class
})
public class FunctionConfiguration {
    // Main configuration - actual functions are defined in imported configuration classes
}

