package de.bbajor.pvs.function.core;

import de.bbajor.pvs.common.function.FunctionRequest;
import de.bbajor.pvs.common.function.FunctionResponse;
import de.bbajor.pvs.common.security.SecurityContext;
import de.bbajor.pvs.institution.service.CurrentInstitutionService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.function.Function;

/**
 * Wrapper for Spring Cloud Functions that handles:
 * - Institution/tenant context extraction and setup
 * - Security context extraction
 * - Error handling
 * - Logging
 * 
 * This wrapper ensures that all functions follow the same pattern for
 * multi-tenant isolation and security.
 */
public class FunctionWrapper {
    
    private static final Logger LOG = LogManager.getLogger(FunctionWrapper.class);
    
    /**
     * Wrap a function to handle institution context and security.
     * 
     * @param function the actual function to wrap
     * @param functionName the name of the function (for logging)
     * @return wrapped function
     */
    public static <TRequest extends FunctionRequest, TResponse extends FunctionResponse> 
            Function<TRequest, TResponse> wrap(
            CurrentInstitutionService currentInstitutionService,
            Function<TRequest, TResponse> function, 
            String functionName) {
        
        return request -> {
            Long institutionId = request.getInstitutionId();
            if (institutionId != null) {
                LOG.debug("Function {} called with institutionId: {}", functionName, institutionId);
            } else {
                LOG.warn("Function {} called without institutionId", functionName);
            }
            try {
                return currentInstitutionService.callWithInstitutionId(institutionId, () -> {
                    SecurityContext securityContext = request.getSecurityContext();
                    if (securityContext != null) {
                        LOG.debug("Function {} called with user: {}", functionName, securityContext.getUsername());
                    }
                    TResponse response = function.apply(request);
                    if (response != null && response.getErrorMessage() == null) {
                        response.setSuccess(true);
                    }
                    return response;
                });
            } catch (Exception e) {
                LOG.error("Error executing function {}: {}", functionName, e.getMessage(), e);
                try {
                    TResponse errorResponse = currentInstitutionService.callWithInstitutionId(institutionId,
                            () -> function.apply(request));
                    if (errorResponse != null) {
                        FunctionResponse.error(errorResponse, "FUNCTION_ERROR",
                                "Error executing function: " + e.getMessage());
                        return errorResponse;
                    }
                } catch (Exception ex) {
                    LOG.error("Failed to create error response", ex);
                }
                throw new RuntimeException("Function execution failed: " + e.getMessage(), e);
            }
        };
    }
}


