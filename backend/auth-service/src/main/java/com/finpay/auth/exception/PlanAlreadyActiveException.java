package com.finpay.auth.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when a user requests to change to the plan they are already on.
 */
@ResponseStatus(HttpStatus.CONFLICT)
public class PlanAlreadyActiveException extends RuntimeException {

    public PlanAlreadyActiveException(String message) {
        super(message);
    }

    public PlanAlreadyActiveException(String message, Throwable cause) {
        super(message, cause);
    }
}
