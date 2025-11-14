package de.bbajor.pvs.function.monitoring;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Metrics collection for serverless functions.
 * 
 * Tracks:
 * - Function execution count
 * - Function execution time
 * - Function errors
 * - Institution-specific metrics
 */
@Component
@RequiredArgsConstructor
public class FunctionMetrics {
    
    private final MeterRegistry meterRegistry;
    
    /**
     * Record function execution.
     */
    public void recordExecution(String functionName, String institutionId, boolean success, long durationMs) {
        Counter.builder("function.executions")
                .tag("function", functionName)
                .tag("institution", institutionId != null ? institutionId : "unknown")
                .tag("status", success ? "success" : "error")
                .register(meterRegistry)
                .increment();
        
        Timer.builder("function.execution.time")
                .tag("function", functionName)
                .tag("institution", institutionId != null ? institutionId : "unknown")
                .register(meterRegistry)
                .record(durationMs, java.util.concurrent.TimeUnit.MILLISECONDS);
    }
    
    /**
     * Record function error.
     */
    public void recordError(String functionName, String institutionId, String errorType) {
        Counter.builder("function.errors")
                .tag("function", functionName)
                .tag("institution", institutionId != null ? institutionId : "unknown")
                .tag("error_type", errorType)
                .register(meterRegistry)
                .increment();
    }
}


