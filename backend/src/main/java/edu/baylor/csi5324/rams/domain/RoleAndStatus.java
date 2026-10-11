package edu.baylor.csi5324.rams.domain;

/** The current role and status of an account (UC Manage Accounts and Roles, steps 4 and 8). */
public record RoleAndStatus(Role role, AccountStatus status) {
}
