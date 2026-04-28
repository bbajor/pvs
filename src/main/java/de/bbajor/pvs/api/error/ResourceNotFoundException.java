package de.bbajor.pvs.api.error;

import org.springframework.http.HttpStatus;
import org.springframework.web.ErrorResponseException;

public class ResourceNotFoundException extends ErrorResponseException {

    public ResourceNotFoundException(String detail) {
        super(HttpStatus.NOT_FOUND, org.springframework.http.ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, detail),
                null);
    }
}

