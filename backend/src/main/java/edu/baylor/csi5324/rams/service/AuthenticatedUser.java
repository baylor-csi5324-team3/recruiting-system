package edu.baylor.csi5324.rams.service;

import edu.baylor.csi5324.rams.domain.Role;

import java.io.Serializable;

/**
 * The signed-in user kept in the session by User Login. The id is the account id, which is also the
 * applicant, recruiter or administrator id that the other end points expect (the role is the subclass).
 */
public record AuthenticatedUser(Long id, String email, Role role, String name) implements Serializable {
}
