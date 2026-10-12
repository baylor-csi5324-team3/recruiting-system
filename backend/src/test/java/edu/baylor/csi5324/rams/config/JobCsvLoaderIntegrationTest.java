package edu.baylor.csi5324.rams.config;

import edu.baylor.csi5324.rams.domain.*;
import edu.baylor.csi5324.rams.repository.*;
import edu.baylor.csi5324.rams.service.JobPostingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

@SpringBootTest(properties = {"spring.datasource.url=jdbc:h2:mem:csv-integration;DB_CLOSE_DELAY=-1", "spring.jpa.show-sql=false"})
@ActiveProfiles("test")
@Import(JobCsvLoaderIntegrationTest.TimeConfig.class)
class JobCsvLoaderIntegrationTest {
    @TestConfiguration
    static class TimeConfig {
        @Bean @Primary Clock csvClock() { return Clock.fixed(Instant.parse("2026-10-12T14:00:00Z"), ZoneId.of("America/Chicago")); }
    }

    @Autowired JobCsvLoader loader;
    @Autowired UserAccountRepository accounts;
    @Autowired RecruiterRepository recruiters;
    @Autowired CompanyRepository companies;
    @MockitoSpyBean JobPostingRepository postings;
    @Autowired PasswordEncoder encoder;
    @Autowired JobPostingService service;
    private Recruiter recruiter;
    private static final String HEADER = "title,description,location,employmentType,closingDate,status,recruiterEmail\n";
    private static final String ROW = "Developer,Build services,Waco,FULL_TIME,2026-12-31,PUBLISHED,recruiter@example.com\n";

    @BeforeEach
    void setup() {
        postings.deleteAll();
        recruiters.deleteAll();
        accounts.deleteAll();
        companies.deleteAll();
        var account = accounts.save(new UserAccount("recruiter@example.com", encoder.encode("CSV fixture secret"), Role.RECRUITER, AccountStatus.ACTIVE));
        recruiter = recruiters.save(new Recruiter("Rhea", account, companies.save(new Company("Baylor Tech"))));
    }

    private ByteArrayResource csv(String content) { return new ByteArrayResource(content.getBytes(StandardCharsets.UTF_8)); }

    @Test
    void classpathImportMapsFieldsRelationshipsAndTimestampsAndPreservesSearch() {
        assertEquals(5, loader.loadIfEmpty());
        var jobs = postings.findAll();
        assertEquals(5, jobs.size());
        var intern = jobs.stream().filter(j -> j.getTitle().equals("Software Engineering Intern")).findFirst().orElseThrow();
        assertTrue(intern.getDescription().contains("\"candidate dashboard\""));
        assertEquals("Austin, TX", intern.getLocation());
        assertEquals("INTERNSHIP", intern.getEmploymentType());
        assertEquals(LocalDate.of(2026, 12, 31), intern.getClosingDate());
        assertEquals(LocalDateTime.of(2026, 10, 12, 8, 0), intern.getPostedDate());
        for (var job : jobs) {
            assertEquals(recruiter.getId(), job.getRecruiter().getId());
            assertEquals(recruiter.getCompany().getId(), job.getCompany().getId());
            if (job.getStatus() == PostingStatus.DRAFT) assertNull(job.getPostedDate());
            else {
                assertTrue(job.getCreatedAt().isBefore(job.getPostedDate()));
                assertTrue(job.getPostedDate().toLocalDate().isBefore(job.getClosingDate()));
            }
        }
        assertEquals(1, jobs.stream().filter(j -> j.getStatus() == PostingStatus.DRAFT).count());
        assertEquals(1, jobs.stream().filter(j -> j.getClosingDate().isBefore(LocalDate.of(2026, 10, 12))).count());
        assertEquals(List.of("Software Engineering Intern", "Backend Developer", "Frontend Developer"),
                service.findOpenPostings(SearchCriteria.NONE).stream().map(JobPosting::getTitle).toList());
        assertEquals(1, accounts.count());
        assertEquals(1, companies.count());
    }

    @Test
    void repeatedInitializationPreservesIdsAndTimestamps() {
        loader.loadIfEmpty();
        var before = postings.findAll().stream().map(j -> List.of(j.getId(), j.getCreatedAt())).toList();
        assertEquals(0, loader.loadIfEmpty());
        assertEquals(before, postings.findAll().stream().map(j -> List.of(j.getId(), j.getCreatedAt())).toList());
    }

    @Test
    void anyExistingJobSkipsImportWithoutReadingResource() {
        var job = service.createDraft(new PostingDetails("User-created", "Keep this", "Waco", "FULL_TIME", LocalDate.of(2026, 12, 31)), recruiter);
        assertEquals(0, loader.loadIfEmpty(csv("invalid CSV")));
        assertEquals(1, postings.count());
        assertEquals("Keep this", postings.findById(job.getId()).orElseThrow().getDescription());
    }

    @Test
    void missingAccountInSecondRowLeavesNoPartialImport() {
        var error = assertThrows(JobCsvException.class, () -> loader.loadIfEmpty(csv(HEADER + ROW + ROW.replace("recruiter@example.com", "missing@example.com"))));
        assertTrue(error.getMessage().contains("row 2"));
        assertEquals(0, postings.count());
        assertEquals(1, accounts.count());
        assertEquals(1, companies.count());
    }

    @Test
    void accountWithoutRecruiterEntityFails() {
        recruiters.deleteAll();
        assertTrue(assertThrows(JobCsvException.class, loader::loadIfEmpty).getMessage().contains("no associated Recruiter"));
        assertEquals(0, postings.count());
    }

    @Test
    void wrongRoleAndInactiveAccountsFail() {
        var account = accounts.findById(recruiter.getAccount().getId()).orElseThrow();
        account.updateRoleAndStatus(Role.APPLICANT, AccountStatus.ACTIVE, null, LocalDateTime.now());
        accounts.saveAndFlush(account);
        assertThrows(JobCsvException.class, loader::loadIfEmpty);
        account.updateRoleAndStatus(Role.RECRUITER, AccountStatus.DEACTIVATED, null, LocalDateTime.now());
        accounts.saveAndFlush(account);
        assertThrows(JobCsvException.class, loader::loadIfEmpty);
        assertEquals(0, postings.count());
    }

    @Test
    void malformedLaterRecordDoesNotPersistEarlierRecord() {
        assertThrows(JobCsvException.class, () -> loader.loadIfEmpty(csv(HEADER + ROW + "\"unclosed")));
        assertEquals(0, postings.count());
    }

    @Test
    void databaseFailureAfterFirstInsertRollsBackWholeImport() {
        doAnswer(invocation -> {
            Iterable<JobPosting> jobs = invocation.getArgument(0);
            postings.saveAndFlush(jobs.iterator().next());
            throw new DataIntegrityViolationException("simulated later insert failure");
        }).when(postings).saveAllAndFlush(any());
        assertThrows(DataIntegrityViolationException.class, loader::loadIfEmpty);
        assertEquals(0, postings.count());
    }
}
