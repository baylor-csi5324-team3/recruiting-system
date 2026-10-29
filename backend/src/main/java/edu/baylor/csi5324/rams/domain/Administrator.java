package edu.baylor.csi5324.rams.domain;

import jakarta.persistence.Entity;

/** Person who manages accounts and roles. Every account change is recorded with the administrator. */
@Entity
public class Administrator extends UserAccount {

    protected Administrator() {
    }

    /** Transient construction for unit tests; an instance without email and password cannot be persisted. */
    public Administrator(String name) {
        super(name, Role.ADMINISTRATOR);
    }

    public Administrator(String email, String passwordHash, String name, AccountStatus status) {
        super(email, passwordHash, name, Role.ADMINISTRATOR, status);
    }

    /** Manage Accounts and Roles, SD 6.14 message 3.1.5.2: the same person, now an administrator. */
    public Administrator(UserAccount previous) {
        super(previous, Role.ADMINISTRATOR);
    }
}
