package edu.baylor.csi5324.rams.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Column;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

/** Employer that owns job postings. Recruiters work for exactly one company. */
@Entity
public class Company {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, columnDefinition = "VARCHAR_IGNORECASE(255)")
    private String name;

    protected Company() {
    }

    public Company(String name) {
        this.name = name;
        normalizeName();
    }

    @PrePersist
    @PreUpdate
    private void normalizeName() {
        if (name != null) name = name.trim();
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
