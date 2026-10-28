package edu.baylor.csi5324.rams.service;

import edu.baylor.csi5324.rams.domain.AccountStatus;
import edu.baylor.csi5324.rams.domain.Administrator;
import edu.baylor.csi5324.rams.domain.Role;
import edu.baylor.csi5324.rams.domain.UserAccount;
import edu.baylor.csi5324.rams.repository.UserAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * Business rules for accounts and roles. Implements the Service lifeline of the sequence diagram
 * for Manage Accounts and Roles (UC-15).
 */
@Service
@Transactional
public class AccountService {

    private final UserAccountRepository accounts;
    private final Clock clock;

    public AccountService(UserAccountRepository accounts, Clock clock) {
        this.accounts = accounts;
        this.clock = clock;
    }

    /** SD 2.1: the account identified by its email address (extension 3a when none matches). */
    @Transactional(readOnly = true)
    public UserAccount findAccount(String email) {
        return accounts.findByEmailIgnoreCase(email == null ? "" : email.trim())          // 2.1.1
                .orElseThrow(() -> new NotFoundException("No account matches " + email + "."));
    }

    /**
     * SD 3.1: validates the change, applies it and records it with the administrator and the time.
     * A null role or status means "keep the current value".
     */
    public UserAccount applyChange(Long accountId, Role newRole, AccountStatus newStatus, Administrator admin) {
        UserAccount account = accounts.findById(accountId)                           // 3.1.1
                .orElseThrow(() -> new NotFoundException("No account with id " + accountId + "."));
        validateChange(newRole, newStatus, account);                                   // 3.1.2
        account.updateRoleAndStatus(newRole, newStatus, admin, LocalDateTime.now(clock)); // 3.1.3
        return accounts.save(account);                                                 // 3.1.4
    }

    private void validateChange(Role newRole, AccountStatus newStatus, UserAccount account) {
        if (newRole == null && newStatus == null) {
            throw new ValidationException("Provide a new role, or activate or deactivate the account.");
        }
        Role resultingRole = newRole != null ? newRole : account.getRole();
        AccountStatus resultingStatus = newStatus != null ? newStatus : account.getStatus();
        if (resultingRole == account.getRole() && resultingStatus == account.getStatus()) {   // extension 6b
            throw new ValidationException("The account is already " + account.getRole() + " and " + account.getStatus() + ".");
        }
        boolean losesActiveAdministrator = account.isActiveAdministrator()             // 3.1.2.1
                && !(resultingRole == Role.ADMINISTRATOR && resultingStatus == AccountStatus.ACTIVE);
        if (losesActiveAdministrator
                && accounts.countByRoleAndStatus(Role.ADMINISTRATOR, AccountStatus.ACTIVE) <= 1) {   // 3.1.2.2, extension 6a
            throw new ValidationException("The change would leave the system without an active administrator.");
        }
    }
}
