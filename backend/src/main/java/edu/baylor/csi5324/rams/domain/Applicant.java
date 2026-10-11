package edu.baylor.csi5324.rams.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

@Entity
public class Applicant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @JsonIgnore
    @OneToOne(optional = false)
    @JoinColumn(
            name = "account_id",
            nullable = false,
            unique = true
    )
    private UserAccount account;

    // JPA 使用的无参构造方法
    protected Applicant() {
    }

    public Applicant(String name, UserAccount account) {
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

    public void setName(String name) {
        this.name = name;
    }
}