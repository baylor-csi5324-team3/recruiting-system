package edu.baylor.csi5324.rams.service;

import edu.baylor.csi5324.rams.domain.AccountStatus;
import edu.baylor.csi5324.rams.domain.Administrator;
import edu.baylor.csi5324.rams.domain.Role;
import edu.baylor.csi5324.rams.domain.UserAccount;
import edu.baylor.csi5324.rams.repository.AdministratorRepository;
import edu.baylor.csi5324.rams.repository.ApplicantRepository;
import edu.baylor.csi5324.rams.repository.ApplicationRepository;
import edu.baylor.csi5324.rams.repository.JobPostingRepository;
import edu.baylor.csi5324.rams.repository.RecruiterRepository;
import edu.baylor.csi5324.rams.repository.UserAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import edu.baylor.csi5324.rams.repository.ApplicationRepository;
import edu.baylor.csi5324.rams.repository.ApplicantRepository;
import static edu.baylor.csi5324.rams.service.FixedClockConfig.TODAY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** UC-15 Manage Accounts and Roles, service layer with H2. */
@SpringBootTest
@ActiveProfiles("test")
@Import(FixedClockConfig.class)
class AccountServiceTest {

    @Autowired AccountService service;
    @Autowired UserAccountRepository accounts;
    @Autowired AdministratorRepository administrators;
    @Autowired RecruiterRepository recruiters;
    @Autowired JobPostingRepository postings;
    @Autowired ApplicationRepository applications;
    @Autowired ApplicantRepository applicants;

    private Administrator admin;
    private UserAccount adminAccount;
    private UserAccount applicant;

    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void setUp() {
        // Delete dependent records before the records they reference.
        applications.deleteAll();
        applicants.deleteAll();
        postings.deleteAll();
        recruiters.deleteAll();

        jdbc.update("update user_account set last_changed_by_id = null");
        administrators.deleteAll();
        accounts.deleteAll();

        adminAccount = accounts.save(
                new UserAccount(
                        "admin@example.com",
                        Role.ADMINISTRATOR,
                        AccountStatus.ACTIVE
                )
        );

        admin = administrators.save(
                new Administrator("Ada", adminAccount)
        );

        applicant = accounts.save(
                new UserAccount(
                        "applicant@example.com",
                        Role.APPLICANT,
                        AccountStatus.ACTIVE
                )
        );
    }

    @Test
    @DisplayName("findAccount matches the email ignoring case; an unknown email is reported (extension 3a)")
    void findAccount() {
        assertEquals(applicant.getId(), service.findAccount("Applicant@Example.com").getId());
        assertThrows(NotFoundException.class, () -> service.findAccount("nobody@example.com"));
    }

    @Test
    @DisplayName("applyChange assigns the new role and records the administrator and the time (steps 7 and 8)")
    void applyChangeAssignsRole() {
        UserAccount updated = service.applyChange(applicant.getId(), Role.RECRUITER, null, admin);

        assertEquals(Role.RECRUITER, updated.getRole());
        assertEquals(AccountStatus.ACTIVE, updated.getStatus());
        assertEquals(admin.getId(), updated.getLastChangedBy().getId());
        assertEquals(TODAY.atTime(9, 0), updated.getLastChangedAt());
    }

    @Test
    @DisplayName("applyChange deactivates and reactivates an account")
    void applyChangeStatus() {
        assertEquals(AccountStatus.DEACTIVATED, service.applyChange(applicant.getId(), null, AccountStatus.DEACTIVATED, admin).getStatus());
        assertEquals(AccountStatus.ACTIVE, service.applyChange(applicant.getId(), null, AccountStatus.ACTIVE, admin).getStatus());
    }

    @Test
    @DisplayName("a change to the status the account already has is rejected (extension 6b)")
    void sameStatusRejected() {
        ValidationException e = assertThrows(ValidationException.class,
                () -> service.applyChange(applicant.getId(), null, AccountStatus.ACTIVE, admin));
        assertTrue(e.getMessage().contains("already"));
        assertThrows(ValidationException.class, () -> service.applyChange(applicant.getId(), Role.APPLICANT, null, admin));
        assertThrows(ValidationException.class, () -> service.applyChange(applicant.getId(), null, null, admin));
    }

    @Test
    @DisplayName("the last active administrator can be neither deactivated nor demoted (extension 6a)")
    void lastAdministratorIsProtected() {
        assertThrows(ValidationException.class, () -> service.applyChange(adminAccount.getId(), null, AccountStatus.DEACTIVATED, admin));
        assertThrows(ValidationException.class, () -> service.applyChange(adminAccount.getId(), Role.APPLICANT, null, admin));
        assertEquals(Role.ADMINISTRATOR, accounts.findById(adminAccount.getId()).orElseThrow().getRole());

        accounts.save(new UserAccount("second.admin@example.com", Role.ADMINISTRATOR, AccountStatus.ACTIVE));
        assertEquals(AccountStatus.DEACTIVATED, service.applyChange(adminAccount.getId(), null, AccountStatus.DEACTIVATED, admin).getStatus());
    }

    @Test
    @DisplayName("an unknown account id is reported")
    void unknownAccount() {
        assertThrows(NotFoundException.class, () -> service.applyChange(999_999L, Role.RECRUITER, null, admin));
    }
}
