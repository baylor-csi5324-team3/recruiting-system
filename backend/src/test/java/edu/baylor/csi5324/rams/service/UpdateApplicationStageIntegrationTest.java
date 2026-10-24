
package edu.baylor.csi5324.rams.service;

import edu.baylor.csi5324.rams.domain.*;
import edu.baylor.csi5324.rams.repository.*;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class UpdateApplicationStageIntegrationTest {

    @Autowired
    private ApplicationService service;

    @Autowired
    private ApplicationRepository applications;

    @Autowired
    private UserAccountRepository accounts;

    @Autowired
    private ApplicantRepository applicants;

    @Autowired
    private CompanyRepository companies;

    @Autowired
    private RecruiterRepository recruiters;

    @Autowired
    private JobPostingRepository jobs;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private PasswordEncoder encoder;

    @Test
    void recruiterStageUpdateIsPersistedAndVisibleToApplicant() {

        // Step 1: Create an applicant.
        UserAccount applicantAccount = accounts.save(
                new UserAccount(
                        "stage-a@example.com",
                        encoder.encode("test password"),
                        Role.APPLICANT,
                        AccountStatus.ACTIVE));

        Applicant applicant = applicants.save(
                new Applicant("Applicant", applicantAccount));

        // Step 2: Create a company and recruiter.
        Company company = companies.save(
                new Company("Stage Test Co"));

        UserAccount recruiterAccount = accounts.save(
                new UserAccount(
                        "stage-r@example.com",
                        encoder.encode("test password"),
                        Role.RECRUITER,
                        AccountStatus.ACTIVE));

        Recruiter recruiter = recruiters.save(
                new Recruiter(
                        "Recruiter",
                        recruiterAccount,
                        company));

        // Step 3: Create a job posting.
        JobPosting posting = jobs.save(
                new JobPosting(
                        new PostingDetails(
                                "Engineer",
                                "Test position",
                                "Waco, TX",
                                "FULL_TIME",
                                LocalDate.now().plusYears(1)),
                        recruiter,
                        LocalDateTime.now()));

        // Step 4: Create a submitted application.
        Application application = applications.saveAndFlush(
                new Application(
                        applicant,
                        posting,
                        LocalDateTime.now()));

        // Step 5: Recruiter updates the application stage.
        service.updateStage(
                application.getId(),
                recruiter.getId(),
                ApplicationStage.UNDER_REVIEW);

        // Step 6: Flush changes and clear persistence context.
        entityManager.flush();
        entityManager.clear();

        // Step 7: Reload the application from the database.
        Application reloaded = applications
                .findById(application.getId())
                .orElseThrow();

        assertEquals(
                ApplicationStage.UNDER_REVIEW,
                reloaded.getStage());

        assertNotNull(reloaded.getStageUpdatedAt());

        // Step 8: Applicant can view the updated stage.
        ApplicationSummary summary =
                service.getApplicationDetails(
                        application.getId(),
                        applicant.getId());

        assertEquals(
                ApplicationStage.UNDER_REVIEW,
                summary.stage());
    }
}
