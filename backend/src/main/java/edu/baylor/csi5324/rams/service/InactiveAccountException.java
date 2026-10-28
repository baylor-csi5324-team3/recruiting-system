package edu.baylor.csi5324.rams.service;

/** User Login extension 5b: a deactivated account is refused at sign in. */
public class InactiveAccountException extends RuntimeException {

    public InactiveAccountException() {
        super("This account is inactive. Contact an administrator.");
    }
}
