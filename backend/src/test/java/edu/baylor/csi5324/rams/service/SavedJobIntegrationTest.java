
package edu.baylor.csi5324.rams.service;

import edu.baylor.csi5324.rams.domain.*;
import edu.baylor.csi5324.rams.repository.*;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:saved-job-integration;DB_CLOSE_DELAY=-1",
        "spring.jpa.show-sql=false",
        "rams.login.enforce=false"
})
@ActiveProfiles("test")
@Import(FixedClockConfig.class)
@Transactional
class SavedJobIntegrationTest {

    @Autowired SavedJobService service;
    @Autowired SavedJobRepository savedJobs;
    @Autowired ApplicantRepository applicants;
    @Autowired ApplicationRepository applications;
    @Autowired CompanyRepository companies;
    @Autowired RecruiterRepository recruiters;
    @Autowired JobPostingRepository jobs;
    @Autowired PasswordEncoder encoder;
    @Autowired EntityManager entityManager;

    private static final LocalDate TODAY =
            FixedClockConfig.TODAY;

    private static final LocalDateTime NOW =
            TODAY.atTime(9, 0);

    private Applicant applicant;
    private Applicant anotherApplicant;
    private Recruiter recruiter;

    @BeforeEach
    void setUp() {

        applicant = applicants.save(new Applicant(
                "saved-job-applicant@example.com",
                encoder.encode("Applicant test password"),
                "Saved Job Applicant",
                AccountStatus.ACTIVE
        ));

        anotherApplicant = applicants.save(new Applicant(
                "saved-job-other@example.com",
                encoder.encode("Applicant test password"),
                "Another Applicant",
                AccountStatus.ACTIVE
        ));

        Company company = companies.save(
                new Company("Saved Job Integration Co")
        );

        recruiter = recruiters.save(new Recruiter(
                "saved-job-recruiter@example.com",
                encoder.encode("Recruiter test password"),
                "Saved Job Recruiter",
                company,
                AccountStatus.ACTIVE
        ));
    }

    private JobPosting createJob(
            LocalDate closingDate,
            PostingStatus status) {

        JobPosting job = new JobPosting(
                new PostingDetails(
                        "Integration Job",
                        "Integration job description",
                        "Waco, TX",
                        "FULL_TIME",
                        closingDate
                ),
                recruiter,
                NOW
        );

        if (status == PostingStatus.PUBLISHED
                || status == PostingStatus.CLOSED) {

            job.markPublished(recruiter, NOW);
        }

        if (status == PostingStatus.CLOSED) {
            job.closePosting();
        }

        return jobs.saveAndFlush(job);
    }

    @Test
    void saveOpenJobPersistsRecordAndRetrievesIt() {

        JobPosting job = createJob(
                TODAY.plusDays(10),
                PostingStatus.PUBLISHED
        );

        long applicationCountBefore = applications.count();

        SavedJob result = service.saveJobPosting(
                applicant.getId(),
                job.getId()
        );

        assertNotNull(result.getId());

        entityManager.flush();
        entityManager.clear();

        SavedJob persisted = savedJobs
                .findById(result.getId())
                .orElseThrow();

        assertEquals(
                applicant.getId(),
                persisted.getApplicant().getId()
        );

        assertEquals(
                job.getId(),
                persisted.getJobPosting().getId()
        );

        assertEquals(NOW, persisted.getSavedAt());

        var resultList =
                service.listSavedJobs(applicant.getId());

        assertEquals(1, resultList.size());

        assertEquals(
                result.getId(),
                resultList.getFirst().getId()
        );

        assertEquals(
                applicationCountBefore,
                applications.count()
        );
    }

    @Test
    void duplicateSavedJobIsRejected() {

        JobPosting job = createJob(
                TODAY.plusDays(10),
                PostingStatus.PUBLISHED
        );

        service.saveJobPosting(
                applicant.getId(),
                job.getId()
        );

        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> service.saveJobPosting(
                        applicant.getId(),
                        job.getId()
                )
        );

        assertEquals(
                "You have already saved this job.",
                exception.getMessage()
        );

        assertEquals(
                1,
                savedJobs.findByApplicantIdOrderBySavedAtDesc(
                        applicant.getId()
                ).size()
        );
    }

    @Test
    void closedJobCannotBeSaved() {

        JobPosting job = createJob(
                TODAY.plusDays(10),
                PostingStatus.CLOSED
        );

        assertThrows(
                ValidationException.class,
                () -> service.saveJobPosting(
                        applicant.getId(),
                        job.getId()
                )
        );

        assertFalse(
                savedJobs.existsByApplicantIdAndJobPostingId(
                        applicant.getId(),
                        job.getId()
                )
        );
    }

    @Test
    void draftJobCannotBeSaved() {

        JobPosting job = createJob(
                TODAY.plusDays(10),
                PostingStatus.DRAFT
        );

        assertThrows(
                ValidationException.class,
                () -> service.saveJobPosting(
                        applicant.getId(),
                        job.getId()
                )
        );

        assertFalse(
                savedJobs.existsByApplicantIdAndJobPostingId(
                        applicant.getId(),
                        job.getId()
                )
        );
    }

    @Test
    void expiredJobCannotBeSaved() {

        JobPosting job = createJob(
                TODAY.minusDays(1),
                PostingStatus.PUBLISHED
        );

        assertThrows(
                ValidationException.class,
                () -> service.saveJobPosting(
                        applicant.getId(),
                        job.getId()
                )
        );

        assertFalse(
                savedJobs.existsByApplicantIdAndJobPostingId(
                        applicant.getId(),
                        job.getId()
                )
        );
    }

    @Test
    void jobClosingTodayCanStillBeSaved() {

        JobPosting job = createJob(
                TODAY,
                PostingStatus.PUBLISHED
        );

        SavedJob result = service.saveJobPosting(
                applicant.getId(),
                job.getId()
        );

        assertNotNull(result.getId());

        assertTrue(
                savedJobs.existsByApplicantIdAndJobPostingId(
                        applicant.getId(),
                        job.getId()
                )
        );
    }

    @Test
    void nonexistentJobIsRejected() {

        assertThrows(
                NotFoundException.class,
                () -> service.saveJobPosting(
                        applicant.getId(),
                        Long.MAX_VALUE
                )
        );
    }

    @Test
    void removeSavedJobDoesNotDeleteJobPosting() {

        JobPosting job = createJob(
                TODAY.plusDays(10),
                PostingStatus.PUBLISHED
        );

        SavedJob result = service.saveJobPosting(
                applicant.getId(),
                job.getId()
        );

        service.removeSavedJob(
                applicant.getId(),
                job.getId()
        );

        entityManager.flush();
        entityManager.clear();

        assertFalse(
                savedJobs.existsById(result.getId())
        );

        assertTrue(
                jobs.existsById(job.getId())
        );

        assertTrue(
                service.listSavedJobs(
                        applicant.getId()
                ).isEmpty()
        );
    }

    @Test
    void previouslySavedJobRemainsAfterClosing() {

        JobPosting job = createJob(
                TODAY.plusDays(10),
                PostingStatus.PUBLISHED
        );

        SavedJob result = service.saveJobPosting(
                applicant.getId(),
                job.getId()
        );

        job.closePosting();
        jobs.saveAndFlush(job);

        entityManager.clear();

        var resultList =
                service.listSavedJobs(applicant.getId());

        assertEquals(1, resultList.size());

        assertEquals(
                result.getId(),
                resultList.getFirst().getId()
        );

        assertEquals(
                PostingStatus.CLOSED,
                resultList.getFirst()
                        .getJobPosting()
                        .getStatus()
        );
    }

    @Test
    void differentApplicantsCanSaveSameJobIndependently() {

        JobPosting job = createJob(
                TODAY.plusDays(10),
                PostingStatus.PUBLISHED
        );

        SavedJob first = service.saveJobPosting(
                applicant.getId(),
                job.getId()
        );

        SavedJob second = service.saveJobPosting(
                anotherApplicant.getId(),
                job.getId()
        );

        assertNotEquals(first.getId(), second.getId());

        assertEquals(
                first.getId(),
                service.listSavedJobs(
                        applicant.getId()
                ).getFirst().getId()
        );

        assertEquals(
                second.getId(),
                service.listSavedJobs(
                        anotherApplicant.getId()
                ).getFirst().getId()
        );

        service.removeSavedJob(
                applicant.getId(),
                job.getId()
        );

        entityManager.flush();
        entityManager.clear();

        assertFalse(
                savedJobs.existsByApplicantIdAndJobPostingId(
                        applicant.getId(),
                        job.getId()
                )
        );

        assertTrue(
                savedJobs.existsByApplicantIdAndJobPostingId(
                        anotherApplicant.getId(),
                        job.getId()
                )
        );
    }

    @Test
    void deactivatedApplicantCannotSaveJob() {

        Applicant inactiveApplicant = applicants.save(
                new Applicant(
                        "saved-job-inactive@example.com",
                        encoder.encode("Applicant test password"),
                        "Inactive Applicant",
                        AccountStatus.DEACTIVATED
                )
        );

        JobPosting job = createJob(
                TODAY.plusDays(10),
                PostingStatus.PUBLISHED
        );

        assertThrows(
                ValidationException.class,
                () -> service.saveJobPosting(
                        inactiveApplicant.getId(),
                        job.getId()
                )
        );

        assertFalse(
                savedJobs.existsByApplicantIdAndJobPostingId(
                        inactiveApplicant.getId(),
                        job.getId()
                )
        );
    }
}
