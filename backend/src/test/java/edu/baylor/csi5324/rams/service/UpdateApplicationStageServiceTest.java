
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
import java.time.ZoneId;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UpdateApplicationStageServiceTest {

    @Mock
    private ApplicationRepository applications;

    @Mock
    private ApplicantRepository applicants;

    @Mock
    private JobPostingRepository jobs;

    @Mock
    private ResumeRepository resumes;

    @Mock
    private RecruiterRepository recruiters;

    private ApplicationService service;
    private Recruiter recruiter;
    private UserAccount account;
    private JobPosting posting;
    private Application application;
    private Company company;

    private static final LocalDateTime NOW =
            LocalDateTime.of(2026, 10, 12, 10, 0);

    @BeforeEach
    void setup() {

        Clock clock = Clock.fixed(
                Instant.parse("2026-10-12T15:00:00Z"),
                ZoneId.of("America/Chicago"));

        service = new ApplicationService(
                applications,
                applicants,
                clock,
                jobs,
                resumes,
                recruiters);

        recruiter = mock(Recruiter.class);
        account = mock(UserAccount.class);
        posting = mock(JobPosting.class);
        application = mock(Application.class);
        company = mock(Company.class);
    }

    /**
     * Configure an active recruiter who belongs
     * to the same company as the application.
     */
    private void activeRecruiterOwnsApplication() {

        when(recruiters.findById(7L))
                .thenReturn(Optional.of(recruiter));

        when(recruiter.getAccount())
                .thenReturn(account);

        when(account.getRole())
                .thenReturn(Role.RECRUITER);

        when(account.getStatus())
                .thenReturn(AccountStatus.ACTIVE);

        when(applications.findById(15L))
                .thenReturn(Optional.of(application));

        when(recruiter.getCompany())
                .thenReturn(company);

        when(application.getJobPosting())
                .thenReturn(posting);

        when(posting.belongsTo(company))
                .thenReturn(true);
    }

    /**
     * Test 1:
     * An authorized recruiter successfully updates
     * and saves the application stage.
     */
    @Test
    void recruiterUpdatesStageAndPersistsIt() {

        activeRecruiterOwnsApplication();

        ApplicationSummary expected =
                new ApplicationSummary(
                        15L,
                        "Engineer",
                        "Baylor Tech",
                        ApplicationStage.UNDER_REVIEW,
                        NOW);

        when(applications.saveAndFlush(application))
                .thenReturn(application);

        when(application.getDetails())
                .thenReturn(expected);

        ApplicationSummary actual =
                service.updateStage(
                        15L,
                        7L,
                        ApplicationStage.UNDER_REVIEW);

        assertEquals(expected, actual);

        verify(application).updateStage(
                eq(ApplicationStage.UNDER_REVIEW),
                eq(NOW),
                isNull());

        verify(applications).saveAndFlush(application);
    }

    /**
     * Test 2:
     * An applicant account cannot update stages.
     */
    @Test
    void rejectsApplicantRole() {

        when(recruiters.findById(7L))
                .thenReturn(Optional.of(recruiter));

        when(recruiter.getAccount())
                .thenReturn(account);

        when(account.getRole())
                .thenReturn(Role.APPLICANT);

        assertThrows(
                ValidationException.class,
                () -> service.updateStage(
                        15L, 7L, ApplicationStage.OFFER));

        verify(applications, never())
                .saveAndFlush(any());
    }

    /**
     * Test 3:
     * A deactivated recruiter cannot update stages.
     */
    @Test
    void rejectsInactiveRecruiter() {

        when(recruiters.findById(7L))
                .thenReturn(Optional.of(recruiter));

        when(recruiter.getAccount())
                .thenReturn(account);

        when(account.getRole())
                .thenReturn(Role.RECRUITER);

        when(account.getStatus())
                .thenReturn(AccountStatus.DEACTIVATED);

        assertThrows(
                ValidationException.class,
                () -> service.updateStage(
                        15L, 7L, ApplicationStage.OFFER));

        verify(applications, never())
                .saveAndFlush(any());
    }

    /**
     * Test 4:
     * A recruiter cannot update applications
     * belonging to another company.
     */
    @Test
    void rejectsRecruiterFromAnotherCompany() {

        when(recruiters.findById(7L))
                .thenReturn(Optional.of(recruiter));

        when(recruiter.getAccount())
                .thenReturn(account);

        when(account.getRole())
                .thenReturn(Role.RECRUITER);

        when(account.getStatus())
                .thenReturn(AccountStatus.ACTIVE);

        when(applications.findById(15L))
                .thenReturn(Optional.of(application));

        when(recruiter.getCompany())
                .thenReturn(company);

        when(application.getJobPosting())
                .thenReturn(posting);

        when(posting.belongsTo(company))
                .thenReturn(false);

        assertThrows(
                ValidationException.class,
                () -> service.updateStage(
                        15L, 7L, ApplicationStage.OFFER));

        verify(application, never())
                .updateStage(any(), any());

        verify(applications, never())
                .saveAndFlush(any());
    }

    /**
     * Test 5:
     * A nonexistent recruiter is rejected.
     */
    @Test
    void unknownRecruiterReturnsNotFound() {

        when(recruiters.findById(7L))
                .thenReturn(Optional.empty());

        assertThrows(
                NotFoundException.class,
                () -> service.updateStage(
                        15L, 7L, ApplicationStage.INTERVIEW));
    }

    /**
     * Test 6:
     * A nonexistent application is rejected.
     */
    @Test
    void unknownApplicationReturnsNotFound() {

        when(recruiters.findById(7L))
                .thenReturn(Optional.of(recruiter));

        when(recruiter.getAccount())
                .thenReturn(account);

        when(account.getRole())
                .thenReturn(Role.RECRUITER);

        when(account.getStatus())
                .thenReturn(AccountStatus.ACTIVE);

        when(applications.findById(15L))
                .thenReturn(Optional.empty());

        assertThrows(
                NotFoundException.class,
                () -> service.updateStage(
                        15L, 7L, ApplicationStage.INTERVIEW));
    }

    /**
     * Test 7:
     * Invalid stage transitions are rejected.
     */
    @Test
    void rejectsInvalidStageTransition() {

        activeRecruiterOwnsApplication();

        doThrow(new IllegalStateException(
                "Invalid application stage transition"))
                .when(application)
                .updateStage(ApplicationStage.ACCEPTED, NOW);

        assertThrows(
                ValidationException.class,
                () -> service.updateStage(
                        15L, 7L, ApplicationStage.ACCEPTED));

        verify(applications, never())
                .saveAndFlush(any());
    }

    /**
     * Test 8:
     * Null stage is rejected without database updates.
     */
    @Test
    void rejectsNullStage() {

        assertThrows(
                ValidationException.class,
                () -> service.updateStage(
                        15L, 7L, null));

        verifyNoInteractions(recruiters, applications);
    }
}
