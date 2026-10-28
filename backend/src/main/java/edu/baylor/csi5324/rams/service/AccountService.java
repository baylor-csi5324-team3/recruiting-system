package edu.baylor.csi5324.rams.service;

import edu.baylor.csi5324.rams.domain.AccountStatus;
import edu.baylor.csi5324.rams.domain.Administrator;
import edu.baylor.csi5324.rams.domain.Applicant;
import edu.baylor.csi5324.rams.domain.Company;
import edu.baylor.csi5324.rams.domain.Recruiter;
import edu.baylor.csi5324.rams.domain.Role;
import edu.baylor.csi5324.rams.domain.UserAccount;
import edu.baylor.csi5324.rams.repository.ApplicationRepository;
import edu.baylor.csi5324.rams.repository.CompanyRepository;
import edu.baylor.csi5324.rams.repository.JobPostingRepository;
import edu.baylor.csi5324.rams.repository.ResumeRepository;
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
    private final ApplicationRepository applications;
    private final ResumeRepository resumes;
    private final JobPostingRepository postings;
    private final CompanyRepository companies;
    private final Clock clock;

    public AccountService(UserAccountRepository accounts, ApplicationRepository applications,
                          ResumeRepository resumes, JobPostingRepository postings,
                          CompanyRepository companies, Clock clock) {
        this.accounts = accounts;
        this.applications = applications;
        this.resumes = resumes;
        this.postings = postings;
        this.companies = companies;
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
        return applyChange(accountId, newRole, newStatus, null, admin);
    }

    /** As above; a new recruiter also needs the company they work for. */
    public UserAccount applyChange(Long accountId, Role newRole, AccountStatus newStatus, String companyName,
                                   Administrator admin) {
        UserAccount account = accounts.findById(accountId)                           // 3.1.1
                .orElseThrow(() -> new NotFoundException("No account with id " + accountId + "."));
        validateChange(newRole, newStatus, account);                                   // 3.1.2
        LocalDateTime now = LocalDateTime.now(clock);
        if (newRole != null && newRole != account.getRole()) {
            return replaceWithRole(account, newRole, newStatus, companyName, admin, now);   // 3.1.3
        }
        account.updateRoleAndStatus(null, newStatus, admin, now);                      // status only: 3.1.3.4 on the same object
        return accounts.save(account);                                                 // and saved in place
    }

    /**
     * The role is the subclass of the account, so a new role means a new object for the same person: the email,
     * password hash, name and status are kept, the old object is deleted. This is only possible while nothing in the
     * system depends on the old role (extension 6c). A new recruiter joins the named company, which is found by name
     * ignoring case or created, as in Register Account. SD 6.14 message 3.1.3 with its sub-messages 3.1.3.1 to 3.1.3.5.
     */
    private UserAccount replaceWithRole(UserAccount account, Role newRole, AccountStatus newStatus, String companyName,
                                        Administrator admin, LocalDateTime time) {
        String company = companyName == null ? "" : companyName.trim();
        if (newRole == Role.RECRUITER && company.isEmpty()) {
            throw new ValidationException("Provide the company the recruiter works for.");
        }
        if (account.getId().equals(admin.getId())) {
            throw new ValidationException("Administrators cannot change their own role.");
        }
        String dependentRecords = dependentRecords(account);                            // 3.1.3.1
        if (dependentRecords != null) {
            throw new ValidationException("The role cannot be changed because the account already has "
                    + dependentRecords + ". Deactivate the account instead.");
        }
        UserAccount replacement = switch (newRole) {                                   // 3.1.3.2 «create»
            case ADMINISTRATOR -> new Administrator(account);
            case APPLICANT -> new Applicant(account);
            case RECRUITER -> new Recruiter(account, companyNamed(company));
        };
        accounts.delete(account);                                                      // 3.1.3.3
        accounts.flush();
        replacement.updateRoleAndStatus(null, newStatus, admin, time);                 // 3.1.3.4
        return accounts.saveAndFlush(replacement);                                     // 3.1.3.5
    }

    private Company companyNamed(String name) {
        return companies.findByNameIgnoreCase(name).orElseGet(() -> companies.saveAndFlush(new Company(name)));
    }

    /** What the account's current role already owns, or null when nothing depends on it. */
    private String dependentRecords(UserAccount account) {
        Long id = account.getId();
        if (account instanceof Applicant) {
            if (!applications.findByApplicantId(id).isEmpty()) return "applications";
            if (!resumes.findByApplicantIdOrderByIdDesc(id).isEmpty()) return "resumes";
        } else if (account instanceof Recruiter) {
            if (postings.existsByRecruiterId(id)) return "job postings";
        } else if (accounts.existsByLastChangedById(id)) {
            return "recorded account changes";
        }
        return null;
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
