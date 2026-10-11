package edu.baylor.csi5324.rams.domain;

/** Whether a {@link UserAccount} may sign in. A DEACTIVATED account is refused at sign in. */
public enum AccountStatus {
    ACTIVE, DEACTIVATED
}
