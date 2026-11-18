package edu.baylor.csi5324.rams.web;

import edu.baylor.csi5324.rams.domain.AccountStatus;
import edu.baylor.csi5324.rams.domain.Administrator;
import edu.baylor.csi5324.rams.domain.Role;
import edu.baylor.csi5324.rams.domain.RoleAndStatus;
import edu.baylor.csi5324.rams.domain.Recruiter;
import edu.baylor.csi5324.rams.domain.RegisterAccountRequest;
import edu.baylor.csi5324.rams.domain.UserAccount;
import edu.baylor.csi5324.rams.repository.UserAccountRepository;
import edu.baylor.csi5324.rams.service.RegistrationService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import java.util.Comparator;
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
 * <p>The administrator is identified by the {@code X-Administrator-Id} header (precondition 1 of the use case);
 * IdentityCheckInterceptor makes sure it is the signed-in user.
 */
@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    public static final String ADMIN_HEADER = "X-Administrator-Id";

    private final AccountService accountService;
    private final AdministratorRepository administrators;
    private final UserAccountRepository accounts;
    private final RegistrationService registration;

    public AccountController(AccountService accountService, AdministratorRepository administrators,
                             UserAccountRepository accounts, RegistrationService registration) {
        this.accountService = accountService;
        this.administrators = administrators;
        this.accounts = accounts;
        this.registration = registration;
    }

    /** The administrator views every registered user with the assigned role and status. */
    @GetMapping
    public List<UserRow> listUsers(@RequestHeader(ADMIN_HEADER) Long adminId) {
        identifiedAdministrator(adminId);
        return accounts.findAll().stream()
                .sorted(Comparator.comparing(UserAccount::getId))
                .map(UserRow::of)
                .toList();
    }

    /** Active users with their role and company, for the signed-in front end (names on pages and menus). */
    @GetMapping("/directory")
    public List<UserRow> directory() {
        return accounts.findAll().stream()
                .filter(UserAccount::isActive)
                .sorted(Comparator.comparing(UserAccount::getId))
                .map(UserRow::of)
                .toList();
    }

    /** The administrator creates a recruiter account for a company. Same checks as Register Account. */
    @PostMapping("/recruiters")
    @ResponseStatus(HttpStatus.CREATED)
    public UserRow createRecruiter(@RequestBody NewRecruiter request,
                                   @RequestHeader(ADMIN_HEADER) Long adminId) {
        identifiedAdministrator(adminId);
        UserAccount created = registration.register(new RegisterAccountRequest(
                request.email(), request.password(), Role.RECRUITER, request.companyName(), request.fullName()));
        return UserRow.of(created);
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

    /** Input for a new recruiter account. */
    public record NewRecruiter(String email, String password, String fullName, String companyName) {
        @Override
        public String toString() {
            return "NewRecruiter[email=" + email + ", password=<redacted>, fullName=" + fullName
                    + ", companyName=" + companyName + "]";
        }
    }

    /** One user in a list: name, email, role, company (recruiters only) and whether the account is active. */
    public record UserRow(Long id, String name, String email, Role role, String company, boolean active) {
        static UserRow of(UserAccount a) {
            String company = a instanceof Recruiter r && r.getCompany() != null ? r.getCompany().getName() : null;
            return new UserRow(a.getId(), a.getName() == null ? a.getEmail() : a.getName(), a.getEmail(),
                    a.getRole(), company, a.getStatus() == AccountStatus.ACTIVE);
        }
    }

    /** Steps 4 and 8: what the system reports about an account. */
    public record AccountView(Long accountId, String email, Role role, AccountStatus status) {
    }
}
