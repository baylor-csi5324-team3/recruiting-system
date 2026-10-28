package edu.baylor.csi5324.rams.service;

import edu.baylor.csi5324.rams.domain.AccountStatus;
import edu.baylor.csi5324.rams.domain.Administrator;
import edu.baylor.csi5324.rams.domain.Applicant;
import edu.baylor.csi5324.rams.domain.Company;
import edu.baylor.csi5324.rams.domain.Recruiter;
import edu.baylor.csi5324.rams.domain.Resume;
import edu.baylor.csi5324.rams.domain.Role;
import edu.baylor.csi5324.rams.domain.UserAccount;
import edu.baylor.csi5324.rams.repository.AdministratorRepository;
import edu.baylor.csi5324.rams.repository.ApplicantRepository;
import edu.baylor.csi5324.rams.repository.ApplicationRepository;
import edu.baylor.csi5324.rams.repository.JobPostingRepository;
import edu.baylor.csi5324.rams.repository.RecruiterRepository;
import edu.baylor.csi5324.rams.repository.ResumeRepository;
import edu.baylor.csi5324.rams.repository.UserAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import static edu.baylor.csi5324.rams.service.FixedClockConfig.TODAY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** UC-15 Manage Accounts and Roles, service layer with H2. */
@SpringBootTest
@ActiveProfiles("test")
@Import(FixedClockConfig.class)
class AccountServiceTest {

    @Autowired org.springframework.security.crypto.password.PasswordEncoder encoder;
    @Autowired AccountService service;
    @Autowired UserAccountRepository accounts;
    @Autowired AdministratorRepository administrators;
    @Autowired RecruiterRepository recruiters;
    @Autowired JobPostingRepository postings;
    @Autowired ApplicationRepository applications;
    @Autowired ApplicantRepository applicants;
    @Autowired ResumeRepository resumes;
    @Autowired edu.baylor.csi5324.rams.repository.CompanyRepository companies;

    private Administrator admin;
    private UserAccount adminAccount;
    private UserAccount applicant;

    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void setUp() {
        // Delete dependent records before the records they reference.
        applications.deleteAll();
        resumes.deleteAll();
        applicants.deleteAll();
        postings.deleteAll();
        recruiters.deleteAll();

        jdbc.update("update user_account set last_changed_by_id = null");
        administrators.deleteAll();
        accounts.deleteAll();

        admin = administrators.save(new Administrator(
                "admin@example.com", encoder.encode("Account fixture secret"), "Ada", AccountStatus.ACTIVE));
        adminAccount = admin;

        applicant = accounts.save(new Applicant(
                "applicant@example.com", encoder.encode("Account fixture secret"), "Ann", AccountStatus.ACTIVE));
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
        UserAccount updated = service.applyChange(applicant.getId(), Role.ADMINISTRATOR, null, admin);

        assertInstanceOf(Administrator.class, updated);
        assertEquals(Role.ADMINISTRATOR, updated.getRole());
        assertEquals("applicant@example.com", updated.getEmail());
        assertEquals("Ann", updated.getName());
        assertTrue(accounts.findById(applicant.getId()).isEmpty(), "the applicant object is replaced");
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

        accounts.save(new Administrator("second.admin@example.com", encoder.encode("Account fixture secret"), "Bea", AccountStatus.ACTIVE));
        assertEquals(AccountStatus.DEACTIVATED, service.applyChange(adminAccount.getId(), null, AccountStatus.DEACTIVATED, admin).getStatus());
    }

    @Test
    @DisplayName("an applicant becomes a recruiter of the named company, found ignoring case")
    void applicantBecomesRecruiterOfTheNamedCompany() {
        Company company = companies.findByNameIgnoreCase("Baylor Tech").orElseGet(() -> companies.save(new Company("Baylor Tech")));

        UserAccount updated = service.applyChange(applicant.getId(), Role.RECRUITER, null, " baylor TECH ", admin);

        Recruiter recruiter = assertInstanceOf(Recruiter.class, updated);
        assertEquals(company.getId(), recruiter.getCompany().getId());
        assertEquals("applicant@example.com", recruiter.getEmail());
        assertEquals(admin.getId(), recruiter.getLastChangedBy().getId());
    }

    @Test
    @DisplayName("a role change is refused when it needs a company or would lose records (extension 6c)")
    void roleChangeRefusedWhenRecordsDependOnTheRole() {
        assertTrue(assertThrows(ValidationException.class,
                () -> service.applyChange(applicant.getId(), Role.RECRUITER, null, admin)).getMessage().contains("company"));
        resumes.save(new Resume((Applicant) applicant, "cv.pdf", new byte[] {1}));
        assertTrue(assertThrows(ValidationException.class,
                () -> service.applyChange(applicant.getId(), Role.ADMINISTRATOR, null, admin)).getMessage().contains("resumes"));
        assertInstanceOf(Applicant.class, accounts.findById(applicant.getId()).orElseThrow());
    }

    @Test
    @DisplayName("an unknown account id is reported")
    void unknownAccount() {
        assertThrows(NotFoundException.class, () -> service.applyChange(999_999L, Role.RECRUITER, null, admin));
    }
}
