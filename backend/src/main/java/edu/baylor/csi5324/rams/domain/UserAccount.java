package edu.baylor.csi5324.rams.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Objects;

/**
 * Sign-in account. The role decides the permissions; the status decides whether sign in is allowed.
 * Every change of role or status is recorded with the administrator and the time (UC Manage Accounts and Roles).
 */
@Entity
public class UserAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, columnDefinition = "VARCHAR_IGNORECASE(255)")
    private String email;

    @JsonIgnore
    @Column(nullable = false, length = 60)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccountStatus status;

    @ManyToOne
    private Administrator lastChangedBy;

    private LocalDateTime lastChangedAt;

    protected UserAccount() {
    }

    /** Legacy transient construction; a passwordless instance cannot be persisted. */
    public UserAccount(String email, Role role, AccountStatus status) {
        this.email = email;
        this.role = role;
        this.status = status;
    }

    /** Creates an account with an already encoded credential; raw passwords never enter the entity. */
    public UserAccount(String email, String passwordHash, Role role, AccountStatus status) {
        this(Objects.requireNonNull(email).trim().toLowerCase(Locale.ROOT), role, status);
        this.passwordHash = Objects.requireNonNull(passwordHash);
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

    /** SD message 3.1.2: applies the change and records who made it and when. */
    public void updateRoleAndStatus(Role newRole, AccountStatus newStatus, Administrator admin, LocalDateTime time) {
        if (newRole != null) {
            this.role = newRole;
        }
        if (newStatus != null) {
            this.status = newStatus;
        }
        this.lastChangedBy = admin;
        this.lastChangedAt = time;
    }

    @JsonIgnore
    public boolean isActiveAdministrator() {
        return role == Role.ADMINISTRATOR && status == AccountStatus.ACTIVE;
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public Role getRole() {
        return role;
    }

    public AccountStatus getStatus() {
        return status;
    }

    public Administrator getLastChangedBy() {
        return lastChangedBy;
    }

    public LocalDateTime getLastChangedAt() {
        return lastChangedAt;
    }
}
