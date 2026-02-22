package com.finpay.auth.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when a user attempts an invalid plan upgrade (e.g. downgrading).
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidPlanUpgradeException extends RuntimeException {

    public InvalidPlanUpgradeException(String message) {
        super(message);
    }

    public InvalidPlanUpgradeException(String message, Throwable cause) {
        super(message, cause);
    }
}
