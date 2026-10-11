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

/**
 * Sign-in account. The role decides the permissions; the status decides whether sign in is allowed.
 * Every change of role or status is recorded with the administrator and the time (UC Manage Accounts and Roles).
 */
@Entity
public class UserAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

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

    public UserAccount(String email, Role role, AccountStatus status) {
        this.email = email;
        this.role = role;
        this.status = status;
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
