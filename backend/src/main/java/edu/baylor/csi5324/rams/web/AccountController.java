package edu.baylor.csi5324.rams.web;

import edu.baylor.csi5324.rams.domain.AccountStatus;
import edu.baylor.csi5324.rams.domain.Administrator;
import edu.baylor.csi5324.rams.domain.Role;
import edu.baylor.csi5324.rams.domain.RoleAndStatus;
import edu.baylor.csi5324.rams.domain.UserAccount;
import edu.baylor.csi5324.rams.repository.AdministratorRepository;
import edu.baylor.csi5324.rams.service.AccountService;
import edu.baylor.csi5324.rams.service.NotFoundException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * UC-15 Manage Accounts and Roles. Controller lifeline of the sequence diagram:
 * 1 startAccountChange, 2 identifyAccount, 2.1 findAccount, 2.2 getRoleAndStatus,
 * 3 requestChange, 3.1 applyChange.
 *
 * <p>Sign in is not implemented yet, so the administrator is identified by the
 * {@code X-Administrator-Id} header (precondition 1 of the use case).
 */
@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    public static final String ADMIN_HEADER = "X-Administrator-Id";

    private final AccountService accountService;
    private final AdministratorRepository administrators;

    public AccountController(AccountService accountService, AdministratorRepository administrators) {
        this.accountService = accountService;
        this.administrators = administrators;
    }

    /** Steps 1 and 2: the administrator starts a change and the system asks which account. */
    @GetMapping("/change")
    public Prompt startAccountChange(@RequestHeader(ADMIN_HEADER) Long adminId) {
        identifiedAdministrator(adminId);
        return new Prompt("Identify the account to change by its email address.", List.of("email"));
    }

    /** Steps 3 and 4: the current role and status of the identified account. */
    @GetMapping("/lookup")
    public AccountView identifyAccount(@RequestParam String email,
                                       @RequestHeader(ADMIN_HEADER) Long adminId) {
        identifiedAdministrator(adminId);
        UserAccount account = accountService.findAccount(email);          // 2.1
        RoleAndStatus current = account.getRoleAndStatus();               // 2.2
        return new AccountView(account.getId(), account.getEmail(), current.role(), current.status());
    }

    /** Steps 5 to 8: applies the requested change and reports the new role and status. */
    @PutMapping("/{accountId}/role-status")
    public AccountView requestChange(@PathVariable Long accountId,
                                     @RequestBody AccountChange change,
                                     @RequestHeader(ADMIN_HEADER) Long adminId) {
        Administrator admin = identifiedAdministrator(adminId);
        UserAccount updated = accountService.applyChange(accountId, change.newRole(), change.newStatus(),
                change.companyName(), admin);                                     // 3.1
        return new AccountView(updated.getId(), updated.getEmail(), updated.getRole(), updated.getStatus());
    }

    private Administrator identifiedAdministrator(Long adminId) {
        return administrators.findById(adminId)
                .orElseThrow(() -> new NotFoundException("No administrator with id " + adminId + "."));
    }

    /** Step 5: a new role, a new status, or both. A missing value keeps the current one; a new recruiter needs a company. */
    public record AccountChange(Role newRole, AccountStatus newStatus, String companyName) {
    }

    /** Steps 4 and 8: what the system reports about an account. */
    public record AccountView(Long accountId, String email, Role role, AccountStatus status) {
    }
}
