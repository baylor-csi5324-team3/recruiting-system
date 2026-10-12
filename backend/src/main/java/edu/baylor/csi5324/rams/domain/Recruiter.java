package edu.baylor.csi5324.rams.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.JoinColumn;
import java.util.Objects;

/** Person who creates and publishes job postings for the company they work for. */
@Entity
public class Recruiter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @JsonIgnore
    @OneToOne(optional = false)
    @JoinColumn(name = "account_id", nullable = false, unique = true)
    private UserAccount account;

    @ManyToOne(optional = false)
    private Company company;

    protected Recruiter() {
    }

    /** Legacy transient construction; an accountless instance cannot be persisted. */
    public Recruiter(String name, Company company) {
        this.name = name;
        this.company = company;
    }

    public Recruiter(String name, UserAccount account, Company company) {
        this(name, Objects.requireNonNull(company));
        this.account = Objects.requireNonNull(account);
    }

    @JsonIgnore
    public UserAccount getAccount() {
        return account;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Company getCompany() {
        return company;
    }
}
