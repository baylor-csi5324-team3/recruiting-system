package edu.baylor.csi5324.rams.service;

import edu.baylor.csi5324.rams.domain.*;
import edu.baylor.csi5324.rams.repository.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApplicationWithdrawalServiceTest {

    @Mock ApplicationRepository applications;
    @Mock ApplicantRepository applicants;
    @Mock JobPostingRepository jobs;
    @Mock ResumeRepository resumes;
    @Mock RecruiterRepository recruiters;

    private ApplicationService service;

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-10-24T15:00:00Z"),
            ZoneOffset.UTC
    );

    private static final LocalDateTime SUBMITTED_AT =
            LocalDateTime.of(2026, 10, 24, 9, 0);

    @BeforeEach
    void setUp() {
        service = new ApplicationService(
                applications,
                applicants,
                CLOCK,
                jobs,
                resumes,
                recruiters
        );
    }

    private Applicant activeApplicant() {
        UserAccount account = mock(UserAccount.class);
        Applicant applicant = mock(Applicant.class);

        when(applicants.findById(1L))
                .thenReturn(Optional.of(applicant));
        when(applicant.getAccount()).thenReturn(account);
        when(account.getRole()).thenReturn(Role.APPLICANT);
        when(account.getStatus()).thenReturn(AccountStatus.ACTIVE);

        return applicant;
    }

    private Application ownedApplication(Applicant applicant) {
        JobPosting job = mock(JobPosting.class);
        Application application =
                new Application(applicant, job, SUBMITTED_AT);

        // Different IDs verify that history uses UserAccount ID.
        when(applicant.getId()).thenReturn(1L);
        when(applicant.getAccount().getId()).thenReturn(101L);
        when(applications.findById(10L))
                .thenReturn(Optional.of(application));

        return application;
    }

    @Test
    void withdrawsOwnedApplicationAndRecordsTimeAndUser() {
        Applicant applicant = activeApplicant();
        Application application = ownedApplication(applicant);

        JobPosting job = application.getJobPosting();
        Company company = mock(Company.class);

        when(job.getTitle()).thenReturn("Backend Developer");
        when(job.getCompany()).thenReturn(company);
        when(company.getName()).thenReturn("Baylor Tech");
        when(applications.saveAndFlush(application))
                .thenReturn(application);

        ApplicationSummary result =
                service.withdrawApplication(10L, 1L);

        assertEquals(ApplicationStage.WITHDRAWN, result.stage());
        assertEquals(
                LocalDateTime.now(CLOCK),
                application.getStageUpdatedAt()
        );

        var history = application.getStageHistory();
        assertEquals(2, history.size());

        StageRecord latest = history.get(history.size() - 1);
        assertEquals(ApplicationStage.WITHDRAWN, latest.getStage());
        assertEquals(LocalDateTime.now(CLOCK), latest.getChangedAt());
        assertEquals(Long.valueOf(101L), latest.getChangedByUserId());

        verify(applications).saveAndFlush(application);
    }

    @Test
    void rejectsApplicationOwnedByAnotherApplicant() {
        Applicant applicant = activeApplicant();
        Application application = mock(Application.class);

        when(applications.findById(10L))
                .thenReturn(Optional.of(application));
        when(application.belongsTo(applicant)).thenReturn(false);

        assertThrows(
                ValidationException.class,
                () -> service.withdrawApplication(10L, 1L)
        );

        verify(application, never()).withdraw(any(), any());
        verify(applications, never()).saveAndFlush(any());
    }

    @Test
    void rejectsInactiveApplicantBeforeLookingUpApplication() {
        UserAccount account = mock(UserAccount.class);
        Applicant applicant = mock(Applicant.class);

        when(applicants.findById(1L))
                .thenReturn(Optional.of(applicant));
        when(applicant.getAccount()).thenReturn(account);
        when(account.getRole()).thenReturn(Role.APPLICANT);
        when(account.getStatus()).thenReturn(AccountStatus.DEACTIVATED);

        assertThrows(
                ValidationException.class,
                () -> service.withdrawApplication(10L, 1L)
        );

        verifyNoInteractions(applications);
    }

    @Test
    void rejectsUnknownApplicant() {
        when(applicants.findById(1L)).thenReturn(Optional.empty());

        assertThrows(
                NotFoundException.class,
                () -> service.withdrawApplication(10L, 1L)
        );

        verifyNoInteractions(applications);
    }

    @Test
    void rejectsUnknownApplication() {
        activeApplicant();

        when(applications.findById(10L)).thenReturn(Optional.empty());

        assertThrows(
                NotFoundException.class,
                () -> service.withdrawApplication(10L, 1L)
        );

        verify(applications, never()).saveAndFlush(any());
    }

    @Test
    void rejectsOfferedApplicationWithoutSavingOrChangingHistory() {
        Applicant applicant = activeApplicant();
        Application application = ownedApplication(applicant);

        application.startReview(SUBMITTED_AT.plusHours(1), 25L);
        application.makeOffer(SUBMITTED_AT.plusHours(2), 25L);

        var previousHistory = application.getStageHistory();
        var previousUpdate = application.getStageUpdatedAt();

        ValidationException error = assertThrows(
                ValidationException.class,
                () -> service.withdrawApplication(10L, 1L)
        );

        assertTrue(error.getMessage().contains("can be withdrawn"));
        assertEquals(ApplicationStage.OFFER, application.getStage());
        assertEquals(previousHistory, application.getStageHistory());
        assertEquals(previousUpdate, application.getStageUpdatedAt());

        verify(applications, never()).saveAndFlush(any());
    }

    @Test
    void rejectsMissingIdentifiers() {
        assertThrows(
                ValidationException.class,
                () -> service.withdrawApplication(null, 1L)
        );

        assertThrows(
                ValidationException.class,
                () -> service.withdrawApplication(10L, null)
        );

        verifyNoInteractions(applications, applicants);
    }
}