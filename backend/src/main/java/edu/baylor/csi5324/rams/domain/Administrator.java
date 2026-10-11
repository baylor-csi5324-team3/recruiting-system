package edu.baylor.csi5324.rams.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;

/** Person who manages accounts and roles. Every account change is recorded with the administrator. */
@Entity
public class Administrator {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    /** Not serialised: the account refers back to the administrator who last changed it. */
    @JsonIgnore
    @OneToOne
    private UserAccount account;

    protected Administrator() {
    }

    public Administrator(String name, UserAccount account) {
        this.name = name;
        this.account = account;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public UserAccount getAccount() {
        return account;
    }
}
