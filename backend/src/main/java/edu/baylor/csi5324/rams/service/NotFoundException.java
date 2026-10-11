package edu.baylor.csi5324.rams.service;

/** The named account, posting or person does not exist. Reported to the actor as HTTP 404. */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }
}
