package edu.baylor.csi5324.rams.web;

import edu.baylor.csi5324.rams.domain.AccountStatus;
import edu.baylor.csi5324.rams.domain.Role;

/** Safe registration result; contains neither a plaintext password nor a password hash. */
public record RegisterAccountResponse(Long accountId, String email, Role role, AccountStatus status) {
}
