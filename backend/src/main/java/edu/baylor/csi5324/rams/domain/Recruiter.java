package edu.baylor.csi5324.rams.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;

import java.util.Objects;

/** Person who creates and publishes job postings for the company they work for. */
@Entity
public class Recruiter extends UserAccount {

    @ManyToOne(optional = false)
    private Company company;

    protected Recruiter() {
    }

    /** Transient construction for unit tests; an instance without email and password cannot be persisted. */
    public Recruiter(String name, Company company) {
        super(name, Role.RECRUITER);
        this.company = company;
    }

    public Recruiter(String email, String passwordHash, String name, Company company, AccountStatus status) {
        super(email, passwordHash, name, Role.RECRUITER, status);
        this.company = Objects.requireNonNull(company);
    }

    /** Manage Accounts and Roles, SD message 3.1.3.2: the same person, now a recruiter of the given company. */
    public Recruiter(UserAccount previous, Company company) {
        super(previous, Role.RECRUITER);
        this.company = Objects.requireNonNull(company);
    }

    public Company getCompany() {
        return company;
    }
}
