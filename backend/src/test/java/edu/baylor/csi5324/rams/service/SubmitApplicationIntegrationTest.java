package edu.baylor.csi5324.rams.service;

import edu.baylor.csi5324.rams.domain.*;
import edu.baylor.csi5324.rams.repository.*;
import edu.baylor.csi5324.rams.web.SubmitApplicationRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/** Persistence integration: Submit -> Repository -> My Applications query. */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class SubmitApplicationIntegrationTest {
    @Autowired ApplicationService service;
    @Autowired ApplicationRepository applications;
    @Autowired ApplicantRepository applicants;
    @Autowired ResumeRepository resumes;
    @Autowired UserAccountRepository accounts;
    @Autowired CompanyRepository companies;
    @Autowired RecruiterRepository recruiters;
    @Autowired JobPostingRepository jobs;
    @Autowired PasswordEncoder encoder;

    @Test
    void submitPersistsRecordAndHistoryReadsSameApplication() {
        Applicant applicant = applicants.save(new Applicant("submit-integration@example.com", encoder.encode("Applicant test password"), "Integration Applicant", AccountStatus.ACTIVE));
        Resume resume = resumes.save(new Resume(applicant, "resume.pdf", new byte[] {1, 2, 3}));
        Company company = companies.save(new Company("Integration Co"));
        Recruiter recruiter = recruiters.save(new Recruiter("submit-integration-recruiter@example.com", encoder.encode("Recruiter test password"), "Integration Recruiter", company, AccountStatus.ACTIVE));
        JobPosting job = new JobPosting(new PostingDetails("Integration Job", "Integration description",
                "Waco, TX", "FULL_TIME", LocalDate.now().plusYears(2)),
                recruiter, LocalDateTime.now());
        job.markPublished(recruiter, LocalDateTime.now());
        job = jobs.save(job);

        var result = service.submitApplication(new SubmitApplicationRequest(
                applicant.getId(), job.getId(), resume.getId(), "Hello!"));

        assertEquals(ApplicationStage.SUBMITTED, result.stage());
        Application saved = applications.findById(result.applicationId()).orElseThrow();
        assertArrayEquals(new byte[] {1, 2, 3}, saved.getSubmittedResume());
        assertEquals("Hello!", saved.getCoverLetter());
        assertEquals(result.applicationId(), service.listApplications(applicant.getId()).getFirst().applicationId());
        assertThrows(ValidationException.class, () -> service.submitApplication(
                new SubmitApplicationRequest(applicant.getId(), saved.getJobPosting().getId(),
                        resume.getId(), "Again")));
    }
}
