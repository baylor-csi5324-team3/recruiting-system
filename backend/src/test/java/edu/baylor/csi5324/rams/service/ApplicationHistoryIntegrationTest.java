package edu.baylor.csi5324.rams.service;

import edu.baylor.csi5324.rams.domain.*;
import edu.baylor.csi5324.rams.repository.*;
import edu.baylor.csi5324.rams.web.ApplicationController;
import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ApplicationHistoryIntegrationTest {

    @Autowired ApplicationRepository applications;
    @Autowired ApplicantRepository applicants;
    @Autowired UserAccountRepository accounts;
    @Autowired CompanyRepository companies;
    @Autowired RecruiterRepository recruiters;
    @Autowired JobPostingRepository jobs;
    @Autowired ApplicationController controller;
    @Autowired EntityManager entityManager;

    private static final LocalDateTime SUBMITTED_AT =
            LocalDateTime.of(2026, 10, 24, 9, 0);

    private Long applicationId;
    private Long applicantId;
    private Long applicantUserId;
    private Long recruiterUserId;

    @BeforeEach
    void setUp() {
        String suffix = UUID.randomUUID().toString();


        Applicant applicant = applicants.save(
                new Applicant("history-applicant-" + suffix + "@example.com", "unused-test-hash", "History Applicant", AccountStatus.ACTIVE)
        );

        Company company = companies.save(new Company("History Company"));


        Recruiter recruiter = recruiters.save(
                new Recruiter("history-recruiter-" + suffix + "@example.com", "unused-test-hash", "History Recruiter", company, AccountStatus.ACTIVE)
        );

        JobPosting job = jobs.save(new JobPosting(
                new PostingDetails(
                        "History Test Job",
                        "Test description",
                        "Waco",
                        "FULL_TIME",
                        SUBMITTED_AT.toLocalDate().plusMonths(1)
                ),
                recruiter,
                SUBMITTED_AT.minusDays(1)
        ));

        applicantId = applicant.getId();
        applicantUserId = applicant.getId();
        recruiterUserId = recruiter.getId();

        Application application =
                new Application(applicant, job, SUBMITTED_AT);

        application.startReview(
                SUBMITTED_AT.plusHours(1),
                recruiterUserId
        );

        application.makeOffer(
                SUBMITTED_AT.plusHours(2),
                recruiterUserId
        );

        applicationId = applications.saveAndFlush(application).getId();

        // Force subsequent reads to load from the database.
        entityManager.clear();
    }

    @Test
    void historySurvivesDatabaseReloadInRecordedOrder() {
        Application reloaded =
                applications.findById(applicationId).orElseThrow();

        List<StageRecord> history = reloaded.getStageHistory();

        assertEquals(ApplicationStage.OFFER, reloaded.getStage());

        assertEquals(
                List.of(
                        ApplicationStage.SUBMITTED,
                        ApplicationStage.UNDER_REVIEW,
                        ApplicationStage.OFFER
                ),
                history.stream().map(StageRecord::getStage).toList()
        );

        assertEquals(
                List.of(
                        SUBMITTED_AT,
                        SUBMITTED_AT.plusHours(1),
                        SUBMITTED_AT.plusHours(2)
                ),
                history.stream().map(StageRecord::getChangedAt).toList()
        );

        assertEquals(applicantUserId, history.get(0).getChangedByUserId());
        assertEquals(recruiterUserId, history.get(1).getChangedByUserId());
        assertEquals(recruiterUserId, history.get(2).getChangedByUserId());
    }

    @Test
    void detailsEndpointReturnsPersistedStageHistory() throws Exception {
        var mvc = MockMvcBuilders.standaloneSetup(controller).build();

        mvc.perform(get("/api/applications/{id}", applicationId)
                        .header("X-Applicant-Id", applicantId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobTitle").value("History Test Job"))
                .andExpect(jsonPath("$.stage").value("OFFER"))
                .andExpect(jsonPath("$.stageHistory.length()").value(3))
                .andExpect(jsonPath("$.stageHistory[0].stage")
                        .value("SUBMITTED"))
                .andExpect(jsonPath("$.stageHistory[1].stage")
                        .value("UNDER_REVIEW"))
                .andExpect(jsonPath("$.stageHistory[2].stage")
                        .value("OFFER"))
                .andExpect(jsonPath("$.stageHistory[0].changedAt")
                        .value("2026-10-24T09:00:00"))
                .andExpect(jsonPath("$.stageHistory[1].changedByUserId")
                        .value(recruiterUserId.intValue()));
    }
}