package edu.baylor.csi5324.rams.service;

/** The request was understood but breaks a business rule. Reported to the actor as HTTP 400. */
public class ValidationException extends RuntimeException {

    public ValidationException(String message) {
        super(message);
    }
}
