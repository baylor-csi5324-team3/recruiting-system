
package edu.baylor.csi5324.rams.web;

import edu.baylor.csi5324.rams.domain.*;
import edu.baylor.csi5324.rams.repository.*;
import edu.baylor.csi5324.rams.service.FixedClockConfig;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:saved-job-controller;DB_CLOSE_DELAY=-1",
        "spring.jpa.show-sql=false",
        "rams.login.enforce=true"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(FixedClockConfig.class)
class SavedJobControllerIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired SavedJobRepository savedJobs;
    @Autowired ApplicantRepository applicants;
    @Autowired CompanyRepository companies;
    @Autowired RecruiterRepository recruiters;
    @Autowired JobPostingRepository jobs;
    @Autowired PasswordEncoder encoder;

    private static final String SECRET =
            "Saved job test password";

    private static final LocalDate TODAY =
            FixedClockConfig.TODAY;

    private static final LocalDateTime NOW =
            TODAY.atTime(9, 0);

    private Applicant ann;
    private Applicant bob;
    private Recruiter recruiter;
    private JobPosting job;

    @BeforeEach
    void setUp() {

        String suffix = UUID.randomUUID().toString();

        ann = applicants.save(new Applicant(
                "saved-ann-" + suffix + "@example.com",
                encoder.encode(SECRET),
                "Ann",
                AccountStatus.ACTIVE
        ));

        bob = applicants.save(new Applicant(
                "saved-bob-" + suffix + "@example.com",
                encoder.encode(SECRET),
                "Bob",
                AccountStatus.ACTIVE
        ));

        Company company = companies.save(
                new Company("Saved Job API Co " + suffix)
        );

        recruiter = recruiters.save(new Recruiter(
                "saved-recruiter-" + suffix + "@example.com",
                encoder.encode(SECRET),
                "Saved Job Recruiter",
                company,
                AccountStatus.ACTIVE
        ));

        job = new JobPosting(
                new PostingDetails(
                        "Backend Developer",
                        "Develop backend services",
                        "Waco, TX",
                        "FULL_TIME",
                        TODAY.plusDays(10)
                ),
                recruiter,
                NOW
        );

        job.markPublished(recruiter, NOW);
        job = jobs.saveAndFlush(job);
    }

    private MockHttpSession signIn(Applicant applicant)
            throws Exception {

        MockHttpSession session = (MockHttpSession)
                mvc.perform(post("/api/accounts/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"email\":\""
                                                + applicant.getEmail()
                                                + "\",\"password\":\""
                                                + SECRET + "\"}"
                                ))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getRequest()
                        .getSession(false);

        assertNotNull(session);

        return session;
    }

    private String savedJobsUrl(Applicant applicant) {

        return "/api/applicants/"
                + applicant.getId()
                + "/saved-jobs";
    }

    private String saveRequest(Long jobPostingId) {

        return "{\"jobPostingId\":"
                + jobPostingId + "}";
    }

    private void saveJob(
            Applicant applicant,
            MockHttpSession session) throws Exception {

        mvc.perform(post(savedJobsUrl(applicant))
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(saveRequest(job.getId())))
                .andExpect(status().isCreated());
    }

    @Test
    void signedInApplicantCanSaveRetrieveAndRemoveJob()
            throws Exception {

        MockHttpSession session = signIn(ann);

        mvc.perform(post(savedJobsUrl(ann))
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(saveRequest(job.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.savedJobId").isNumber())
                .andExpect(jsonPath("$.jobPostingId")
                        .value(job.getId().intValue()))
                .andExpect(jsonPath("$.jobTitle")
                        .value("Backend Developer"))
                .andExpect(jsonPath("$.companyName")
                        .value(recruiter.getCompany().getName()))
                .andExpect(jsonPath("$.location")
                        .value("Waco, TX"))
                .andExpect(jsonPath("$.employmentType")
                        .value("FULL_TIME"))
                .andExpect(jsonPath("$.description")
                        .value("Develop backend services"))
                .andExpect(jsonPath("$.status")
                        .value("PUBLISHED"))
                .andExpect(jsonPath("$.closingDate")
                        .value(TODAY.plusDays(10).toString()))
                .andExpect(jsonPath("$.postedDate").exists())
                .andExpect(jsonPath("$.savedAt").exists())
                .andExpect(jsonPath("$.open").value(true));

        assertTrue(
                savedJobs.existsByApplicantIdAndJobPostingId(
                        ann.getId(), job.getId())
        );

        mvc.perform(get(savedJobsUrl(ann))
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].jobPostingId")
                        .value(job.getId().intValue()))
                .andExpect(jsonPath("$[0].open").value(true));

        mvc.perform(delete(
                        savedJobsUrl(ann) + "/" + job.getId())
                        .session(session))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        assertFalse(
                savedJobs.existsByApplicantIdAndJobPostingId(
                        ann.getId(), job.getId())
        );

        assertTrue(jobs.existsById(job.getId()));

        mvc.perform(get(savedJobsUrl(ann))
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void duplicateSaveReturnsBadRequest() throws Exception {

        MockHttpSession session = signIn(ann);

        saveJob(ann, session);

        mvc.perform(post(savedJobsUrl(ann))
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(saveRequest(job.getId())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("You have already saved this job."));

        assertEquals(
                1,
                savedJobs.findByApplicantIdOrderBySavedAtDesc(
                        ann.getId()).size()
        );
    }

    @Test
    void nonexistentJobReturnsNotFound() throws Exception {

        mvc.perform(post(savedJobsUrl(ann))
                        .session(signIn(ann))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(saveRequest(Long.MAX_VALUE)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message")
                        .value("Job posting not found."));
    }

    @Test
    void closedJobCannotBeSaved() throws Exception {

        job.closePosting();
        jobs.saveAndFlush(job);

        mvc.perform(post(savedJobsUrl(ann))
                        .session(signIn(ann))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(saveRequest(job.getId())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Job posting is not open for saving."));

        assertFalse(
                savedJobs.existsByApplicantIdAndJobPostingId(
                        ann.getId(), job.getId())
        );
    }

    @Test
    void previouslySavedClosedJobRemainsVisible()
            throws Exception {

        MockHttpSession session = signIn(ann);

        saveJob(ann, session);

        job.closePosting();
        jobs.saveAndFlush(job);

        mvc.perform(get(savedJobsUrl(ann))
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].jobPostingId")
                        .value(job.getId().intValue()))
                .andExpect(jsonPath("$[0].status")
                        .value("CLOSED"))
                .andExpect(jsonPath("$[0].open")
                        .value(false));

        mvc.perform(delete(
                        savedJobsUrl(ann) + "/" + job.getId())
                        .session(session))
                .andExpect(status().isNoContent());

        assertFalse(
                savedJobs.existsByApplicantIdAndJobPostingId(
                        ann.getId(), job.getId())
        );
    }

    @Test
    void unauthenticatedRequestsAreRejected()
            throws Exception {

        mvc.perform(get(savedJobsUrl(ann)))
                .andExpect(status().isUnauthorized());

        mvc.perform(post(savedJobsUrl(ann))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(saveRequest(job.getId())))
                .andExpect(status().isUnauthorized());

        mvc.perform(delete(
                        savedJobsUrl(ann) + "/" + job.getId()))
                .andExpect(status().isUnauthorized());

        assertFalse(
                savedJobs.existsByApplicantIdAndJobPostingId(
                        ann.getId(), job.getId())
        );
    }

    @Test
    void applicantCannotReadAnotherApplicantsSavedJobs()
            throws Exception {

        saveJob(bob, signIn(bob));

        MockHttpSession annSession = signIn(ann);

        mvc.perform(get(savedJobsUrl(bob))
                        .session(annSession))
                .andExpect(status().isForbidden());

        mvc.perform(get(savedJobsUrl(ann))
                        .session(annSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void applicantCannotSaveOnBehalfOfAnotherApplicant()
            throws Exception {

        mvc.perform(post(savedJobsUrl(bob))
                        .session(signIn(ann))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(saveRequest(job.getId())))
                .andExpect(status().isForbidden());

        assertFalse(
                savedJobs.existsByApplicantIdAndJobPostingId(
                        bob.getId(), job.getId())
        );
    }

    @Test
    void applicantCannotDeleteAnotherApplicantsSavedJob()
            throws Exception {

        saveJob(bob, signIn(bob));

        mvc.perform(delete(
                        savedJobsUrl(bob) + "/" + job.getId())
                        .session(signIn(ann)))
                .andExpect(status().isForbidden());

        assertTrue(
                savedJobs.existsByApplicantIdAndJobPostingId(
                        bob.getId(), job.getId())
        );
    }
}
