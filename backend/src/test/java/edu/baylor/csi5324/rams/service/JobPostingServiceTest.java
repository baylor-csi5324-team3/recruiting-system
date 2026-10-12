package edu.baylor.csi5324.rams.service;

import edu.baylor.csi5324.rams.domain.Company;
import edu.baylor.csi5324.rams.domain.JobPosting;
import edu.baylor.csi5324.rams.domain.PostingDetails;
import edu.baylor.csi5324.rams.domain.PostingStatus;
import edu.baylor.csi5324.rams.domain.Recruiter;
import edu.baylor.csi5324.rams.domain.SearchCriteria;
import edu.baylor.csi5324.rams.repository.CompanyRepository;
import edu.baylor.csi5324.rams.repository.ApplicantRepository;
import edu.baylor.csi5324.rams.repository.ApplicationRepository;
import edu.baylor.csi5324.rams.repository.JobPostingRepository;
import edu.baylor.csi5324.rams.repository.RecruiterRepository;
import edu.baylor.csi5324.rams.repository.ResumeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static edu.baylor.csi5324.rams.service.FixedClockConfig.TODAY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** UC-01 Search Available Jobs and UC-09 Create and Publish a Job Posting, service layer with H2. */
@SpringBootTest
@ActiveProfiles("test")
@Import(FixedClockConfig.class)
class JobPostingServiceTest {

    @Autowired edu.baylor.csi5324.rams.repository.UserAccountRepository accounts;
    @Autowired org.springframework.security.crypto.password.PasswordEncoder encoder;
    @Autowired JobPostingService service;
    @Autowired JobPostingRepository postings;
    @Autowired ApplicationRepository applications;
    @Autowired ApplicantRepository applicants;
    @Autowired ResumeRepository resumes;
    @Autowired CompanyRepository companies;
    @Autowired RecruiterRepository recruiters;
    private Recruiter recruiter;
    private Recruiter otherRecruiter;

    @BeforeEach
    void setUp() {
        applications.deleteAll();
        resumes.deleteAll();
        applicants.deleteAll();
        postings.deleteAll();
        recruiters.deleteAll();
        companies.deleteAll();

        // 后面的创建 Company、Recruiter 的代码保持原样

        Company company = companies.save(new Company("Baylor Tech"));
        Company other = companies.save(new Company("Other Corp"));
        var rhea = accounts.findByEmailIgnoreCase("job-tests.rhea@example.com").orElseGet(() -> accounts.save(
                new edu.baylor.csi5324.rams.domain.UserAccount("job-tests.rhea@example.com", encoder.encode("Job fixture secret"),
                        edu.baylor.csi5324.rams.domain.Role.RECRUITER, edu.baylor.csi5324.rams.domain.AccountStatus.ACTIVE)));
        var omar = accounts.findByEmailIgnoreCase("job-tests.omar@example.com").orElseGet(() -> accounts.save(
                new edu.baylor.csi5324.rams.domain.UserAccount("job-tests.omar@example.com", encoder.encode("Job fixture secret"),
                        edu.baylor.csi5324.rams.domain.Role.RECRUITER, edu.baylor.csi5324.rams.domain.AccountStatus.ACTIVE)));
        recruiter = recruiters.save(new Recruiter("Rhea", rhea, company));
        otherRecruiter = recruiters.save(new Recruiter("Omar", omar, other));
    }

    private JobPosting publishedPosting(String title, String location, String type, LocalDate closing, LocalDateTime postedAt) {
        JobPosting posting = new JobPosting(new PostingDetails(title, "desc", location, type, closing), recruiter, postedAt);
        posting.markPublished(recruiter, postedAt);
        return postings.save(posting);
    }

    @Test
    @DisplayName("findOpenPostings returns open matches only, most recently posted first (steps 5 and 6)")
    void findOpenPostingsFiltersAndSorts() {
        LocalDateTime now = TODAY.atTime(9, 0);
        publishedPosting("Backend Developer", "Waco, TX", "FULL_TIME", TODAY.plusDays(30), now.minusDays(3));
        publishedPosting("Backend Intern", "Waco, TX", "INTERNSHIP", TODAY.plusDays(30), now.minusDays(1));
        publishedPosting("Backend Lead", "Waco, TX", "FULL_TIME", TODAY.minusDays(1), now.minusDays(2));   // closed
        postings.save(new JobPosting(new PostingDetails("Backend Draft", "desc", "Waco, TX", "FULL_TIME", TODAY.plusDays(30)), recruiter, now)); // draft
        publishedPosting("Frontend Developer", "Waco, TX", "FULL_TIME", TODAY.plusDays(30), now);

        List<JobPosting> result = service.findOpenPostings(new SearchCriteria("backend", "Waco, TX", null));

        assertEquals(List.of("Backend Intern", "Backend Developer"), result.stream().map(JobPosting::getTitle).toList());
    }

    @Test
    @DisplayName("no criteria returns all open postings (extension 3a); no match returns an empty list (5a)")
    void noCriteriaAndNoMatch() {
        LocalDateTime now = TODAY.atTime(9, 0);
        publishedPosting("A", "Waco, TX", "FULL_TIME", TODAY.plusDays(1), now.minusDays(1));
        publishedPosting("B", "Austin, TX", "PART_TIME", TODAY.plusDays(1), now);

        assertEquals(List.of("B", "A"), service.findOpenPostings(SearchCriteria.NONE).stream().map(JobPosting::getTitle).toList());
        assertTrue(service.findOpenPostings(new SearchCriteria("nothing-like-this", null, null)).isEmpty());
    }

    @Test
    @DisplayName("createDraft saves a DRAFT that belongs to the recruiter's company and records the creation time")
    void createDraft() {
        JobPosting draft = service.createDraft(new PostingDetails("QA Engineer", "desc", "Waco, TX", "FULL_TIME", TODAY.plusDays(20)), recruiter);

        assertNotNull(draft.getId());
        assertEquals(PostingStatus.DRAFT, draft.getStatus());
        assertEquals("Baylor Tech", draft.getCompany().getName());
        assertEquals(TODAY.atTime(9, 0), draft.getCreatedAt());
        assertTrue(service.findOpenPostings(SearchCriteria.NONE).isEmpty(), "a draft is not returned by searches (extension 5a of UC-09)");
    }

    @Test
    @DisplayName("publish records PUBLISHED with the recruiter and the time, and the posting then appears in searches")
    void publishDraft() {
        JobPosting draft = service.createDraft(new PostingDetails("QA Engineer", "desc", "Waco, TX", "FULL_TIME", TODAY.plusDays(20)), recruiter);

        JobPosting published = service.publish(draft.getId(), recruiter);

        assertEquals(PostingStatus.PUBLISHED, published.getStatus());
        assertEquals(TODAY.atTime(9, 0), published.getPostedDate());
        assertEquals(recruiter.getId(), published.getRecruiter().getId());
        assertEquals(1, service.findOpenPostings(new SearchCriteria("qa", null, null)).size());
    }

    @Test
    @DisplayName("publish rejects a draft whose closing date has passed (extension 6a)")
    void publishRejectsPassedClosingDate() {
        JobPosting draft = postings.save(new JobPosting(new PostingDetails("Old", "desc", "Waco, TX", "FULL_TIME", TODAY), recruiter, TODAY.atTime(8, 0)));

        ValidationException e = assertThrows(ValidationException.class, () -> service.publish(draft.getId(), recruiter));

        assertTrue(e.getMessage().contains("closing date"));
        assertEquals(PostingStatus.DRAFT, postings.findById(draft.getId()).orElseThrow().getStatus());
    }

    @Test
    @DisplayName("publish rejects a posting of another company, a posting that is not a draft, and an unknown id")
    void publishGuards() {
        JobPosting draft = service.createDraft(new PostingDetails("QA Engineer", "desc", "Waco, TX", "FULL_TIME", TODAY.plusDays(20)), recruiter);

        assertThrows(ValidationException.class, () -> service.publish(draft.getId(), otherRecruiter));
        service.publish(draft.getId(), recruiter);
        assertThrows(ValidationException.class, () -> service.publish(draft.getId(), recruiter));
        assertThrows(NotFoundException.class, () -> service.publish(999_999L, recruiter));
    }
}
