package de.bbajor.pvs.api.error;

import java.net.URI;
import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.validation.BindException;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import de.bbajor.pvs.base.util.DsgvoCompliantException;
import de.bbajor.pvs.institution.service.InstitutionAccessViolationException;
import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice
public class GlobalApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalApiExceptionHandler.class);

    @ExceptionHandler(AuthenticationCredentialsNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleUnauthenticated(
            AuthenticationCredentialsNotFoundException ex,
            HttpServletRequest request) {
        return problem(HttpStatus.UNAUTHORIZED, "unauthorized", ex.getMessage(), request, ex, false);
    }

    @ExceptionHandler({ AccessDeniedException.class, InstitutionAccessViolationException.class })
    public ResponseEntity<ProblemDetail> handleForbidden(RuntimeException ex, HttpServletRequest request) {
        return problem(HttpStatus.FORBIDDEN, "forbidden", "Access denied.", request, ex, true);
    }

    @ExceptionHandler({ MethodArgumentNotValidException.class, BindException.class })
    public ResponseEntity<ProblemDetail> handleValidation(Exception ex, HttpServletRequest request) {
        return problem(HttpStatus.BAD_REQUEST, "validation_error", "Validation failed.", request, ex, true);
    }

    @ExceptionHandler(DsgvoCompliantException.class)
    public ResponseEntity<ProblemDetail> handleDsgvo(DsgvoCompliantException ex, HttpServletRequest request) {
        return problem(HttpStatus.BAD_REQUEST, "bad_request", ex.getMessage(), request, ex, false);
    }

    @ExceptionHandler(ErrorResponseException.class)
    public ResponseEntity<ProblemDetail> handleErrorResponse(ErrorResponseException ex, HttpServletRequest request) {
        ProblemDetail pd = ex.getBody();
        if (pd.getType() == null) {
            pd.setType(URI.create("about:blank"));
        }
        pd.setProperty("timestamp", Instant.now().toString());
        pd.setProperty("path", request.getRequestURI());
        return ResponseEntity.status(ex.getStatusCode()).body(pd);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleUnhandled(Exception ex, HttpServletRequest request) {
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "internal_error",
                "Unexpected error.", request, ex, true);
    }

    private ResponseEntity<ProblemDetail> problem(
            HttpStatus status,
            String type,
            String detail,
            HttpServletRequest request,
            Exception ex,
            boolean logStacktrace) {
        if (logStacktrace) {
            log.warn("API error {} {}: {}", status.value(), request.getRequestURI(), ex.getMessage(), ex);
        } else {
            log.warn("API error {} {}: {}", status.value(), request.getRequestURI(), ex.getMessage());
        }

        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
        pd.setTitle(status.getReasonPhrase());
        pd.setType(URI.create("urn:pvs:error:" + type));
        pd.setProperty("timestamp", Instant.now().toString());
        pd.setProperty("path", request.getRequestURI());
        return ResponseEntity.status(status).body(pd);
    }
}

