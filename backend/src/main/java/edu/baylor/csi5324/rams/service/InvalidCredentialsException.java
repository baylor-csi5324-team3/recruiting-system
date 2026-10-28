package edu.baylor.csi5324.rams.service;

/** User Login extension 5a. One message for both cases, so the response does not reveal which e-mails exist. */
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        super("The email or password is incorrect. Please try again.");
    }
}
