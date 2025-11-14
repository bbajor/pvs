package de.bbajor.pvs.common.function;

import org.jspecify.annotations.Nullable;

/**
 * Base class for all function responses.
 * 
 * All function responses should extend this class to ensure:
 * - Consistent response structure
 * - Error handling
 * - Success/failure indication
 */
public abstract class FunctionResponse {
    
    private boolean success = true;
    private @Nullable String errorMessage;
    private @Nullable String errorCode;
    
    public boolean isSuccess() {
        return success;
    }
    
    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    public @Nullable String getErrorMessage() {
        return errorMessage;
    }
    
    public void setErrorMessage(@Nullable String errorMessage) {
        this.errorMessage = errorMessage;
        if (errorMessage != null) {
            this.success = false;
        }
    }
    
    public @Nullable String getErrorCode() {
        return errorCode;
    }
    
    public void setErrorCode(@Nullable String errorCode) {
        this.errorCode = errorCode;
    }
    
    /**
     * Create a success response.
     */
    public static <T extends FunctionResponse> T success(T response) {
        response.setSuccess(true);
        return response;
    }
    
    /**
     * Create an error response.
     */
    public static <T extends FunctionResponse> T error(T response, String errorMessage) {
        response.setSuccess(false);
        response.setErrorMessage(errorMessage);
        return response;
    }
    
    /**
     * Create an error response with error code.
     */
    public static <T extends FunctionResponse> T error(T response, String errorCode, String errorMessage) {
        response.setSuccess(false);
        response.setErrorCode(errorCode);
        response.setErrorMessage(errorMessage);
        return response;
    }
}


