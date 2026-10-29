package edu.baylor.csi5324.rams.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.ManyToOne;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Objects;

/**
 * Abstract superclass of Applicant, Recruiter and Administrator, as in the domain model: the attributes that every
 * user shares (email, name, status). The role is the subclass, so it never changes on an existing object; Manage
 * Accounts and Roles replaces the object instead. Every change of role or status is recorded with the administrator
 * and the time.
 */
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class UserAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @Column(nullable = false, unique = true, columnDefinition = "VARCHAR_IGNORECASE(255)")
    private String email;

    @JsonIgnore
    @Column(nullable = false, length = 60)
    private String passwordHash;

    private String name;

    /** Mirrors the subclass so that accounts can be counted and listed by role. */
    @JsonIgnore
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private Role role;

    @JsonIgnore
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccountStatus status;

    @JsonIgnore
    @ManyToOne
    private Administrator lastChangedBy;

    @JsonIgnore
    private LocalDateTime lastChangedAt;

    protected UserAccount() {
    }

    /** Transient construction for unit tests; an instance without email and password cannot be persisted. */
    protected UserAccount(String name, Role role) {
        this.name = name;
        this.role = role;
        this.status = AccountStatus.ACTIVE;
    }

    /** Creates an account with an already encoded credential; raw passwords never enter the entity. */
    protected UserAccount(String email, String passwordHash, String name, Role role, AccountStatus status) {
        this.email = Objects.requireNonNull(email).trim().toLowerCase(Locale.ROOT);
        this.passwordHash = Objects.requireNonNull(passwordHash);
        this.name = name;
        this.role = Objects.requireNonNull(role);
        this.status = Objects.requireNonNull(status);
    }

    /** The same person with another role: the sign-in details, name and status are kept. */
    protected UserAccount(UserAccount previous, Role role) {
        this(previous.email, previous.passwordHash, previous.name, role, previous.status);
    }

    @JsonIgnore
    public String getPasswordHash() {
        return passwordHash;
    }

    /** SD message 2.2: reports the current role and status. */
    @JsonIgnore
    public RoleAndStatus getRoleAndStatus() {
        return new RoleAndStatus(role, status);
    }

    /**
     * SD 6.14 messages 3.1.3 and 3.1.5.4: applies the change and records who made it and when. The role is fixed by the subclass,
     * so only the current role is accepted here; AccountService replaces the object for a new role.
     */
    public void updateRoleAndStatus(Role newRole, AccountStatus newStatus, Administrator admin, LocalDateTime time) {
        if (newRole != null && newRole != role) {
            throw new IllegalArgumentException("The role of an account is fixed by its type.");
        }
        if (newStatus != null) {
            this.status = newStatus;
        }
        this.lastChangedBy = admin;
        this.lastChangedAt = time;
    }

    /** SD message 3.1.2.1: an active administrator account (asked before the administrator count). */
    @JsonIgnore
    public boolean isActiveAdministrator() {
        return role == Role.ADMINISTRATOR && status == AccountStatus.ACTIVE;
    }

    @JsonIgnore
    public boolean isActive() {
        return status == AccountStatus.ACTIVE;
    }

    public Long getId() {
        return id;
    }

    @JsonIgnore
    public String getEmail() {
        return email;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @JsonIgnore
    public Role getRole() {
        return role;
    }

    @JsonIgnore
    public AccountStatus getStatus() {
        return status;
    }

    @JsonIgnore
    public Administrator getLastChangedBy() {
        return lastChangedBy;
    }

    @JsonIgnore
    public LocalDateTime getLastChangedAt() {
        return lastChangedAt;
    }
}
