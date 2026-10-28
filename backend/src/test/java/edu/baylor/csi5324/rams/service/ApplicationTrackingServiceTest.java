
package edu.baylor.csi5324.rams.service;

import edu.baylor.csi5324.rams.domain.*;
import edu.baylor.csi5324.rams.repository.ApplicantRepository;
import edu.baylor.csi5324.rams.repository.ApplicationRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApplicationTrackingServiceTest {

    @Mock
    private ApplicationRepository applications;

    @Mock
    private ApplicantRepository applicants;

    @Mock
    private Clock clock;

    @InjectMocks
    private ApplicationService service;


    @Test
    void getApplicationsByApplicant_NoApplications() {

        // Create a mock applicant and account.
        Applicant applicant = mock(Applicant.class);

        // Simulate finding applicant ID 1.
        when(applicants.findById(1L))
                .thenReturn(Optional.of(applicant));

        // Simulate an active applicant account.
        when(applicant.getRole()).thenReturn(Role.APPLICANT);
        when(applicant.getStatus()).thenReturn(AccountStatus.ACTIVE);

        // Simulate no applications in the database.
        when(applications.findByApplicantId(1L))
                .thenReturn(List.of());

        // Call the method being tested.
        List<ApplicationSummary> result =
                service.getApplicationsByApplicant(1L);

        // Verify that the returned list is empty.
        assertTrue(result.isEmpty());

        // Verify the repository was queried for applicant ID 1.
        verify(applications).findByApplicantId(1L);
    }


    @Test
    void getApplicationsByApplicant_ReturnsApplications() {

        // Create a mock applicant and account.
        Applicant applicant = mock(Applicant.class);

        // Simulate an active applicant with ID 1.
        when(applicants.findById(1L))
                .thenReturn(Optional.of(applicant));

        when(applicant.getRole()).thenReturn(Role.APPLICANT);
        when(applicant.getStatus()).thenReturn(AccountStatus.ACTIVE);

        // Create two mock applications.
        Application application1 = mock(Application.class);
        Application application2 = mock(Application.class);

        // Create application summaries with different stages.
        ApplicationSummary summary1 = new ApplicationSummary(
                10L, "Software Engineer", "Baylor Tech",
                ApplicationStage.SUBMITTED, null
        );

        ApplicationSummary summary2 = new ApplicationSummary(
                11L, "Backend Developer", "Baylor Tech",
                ApplicationStage.UNDER_REVIEW, null
        );

        // Configure application details.
        when(application1.getDetails()).thenReturn(summary1);
        when(application2.getDetails()).thenReturn(summary2);

        // Simulate retrieving both applications.
        when(applications.findByApplicantId(1L))
                .thenReturn(List.of(application1, application2));

        // Call the service method.
        List<ApplicationSummary> result =
                service.getApplicationsByApplicant(1L);

        // Verify both applications were returned.
        assertEquals(2, result.size());

        // Verify the correct stages.
        assertEquals(ApplicationStage.SUBMITTED, result.get(0).stage());
        assertEquals(ApplicationStage.UNDER_REVIEW, result.get(1).stage());

        // Verify the repository was queried correctly.
        verify(applications).findByApplicantId(1L);
    }


    @Test
    void getApplicationDetails_ReturnsDetails() {

        // Create a mock applicant and account.
        Applicant applicant = mock(Applicant.class);

        // Simulate an active applicant with ID 1.
        when(applicants.findById(1L))
                .thenReturn(Optional.of(applicant));

        when(applicant.getRole()).thenReturn(Role.APPLICANT);
        when(applicant.getStatus()).thenReturn(AccountStatus.ACTIVE);

        // Create a mock application.
        Application application = mock(Application.class);

        // Create the expected application details.
        ApplicationSummary summary = new ApplicationSummary(
                10L, "Software Engineer", "Baylor Tech",
                ApplicationStage.INTERVIEW, null
        );

        // Simulate retrieving application ID 10.
        when(applications.findById(10L))
                .thenReturn(Optional.of(application));

        // Verify the application belongs to this applicant.
        when(application.belongsTo(applicant)).thenReturn(true);

        // Return the application's details.
        when(application.getDetails()).thenReturn(summary);

        // Call the service method.
        ApplicationSummary result =
                service.getApplicationDetails(10L, 1L);

        // Verify the correct application details.
        assertEquals(10L, result.applicationId());
        assertEquals("Software Engineer", result.jobTitle());
        assertEquals("Baylor Tech", result.companyName());
        assertEquals(ApplicationStage.INTERVIEW, result.stage());

        // Verify the correct application was retrieved.
        verify(applications).findById(10L);
    }


    @Test
    void getApplicationDetails_WrongApplicant() {

        // Create a mock applicant and account.
        Applicant applicant = mock(Applicant.class);

        // Simulate an active applicant with ID 1.
        when(applicants.findById(1L))
                .thenReturn(Optional.of(applicant));

        when(applicant.getRole()).thenReturn(Role.APPLICANT);
        when(applicant.getStatus()).thenReturn(AccountStatus.ACTIVE);

        // Create an application belonging to another applicant.
        Application application = mock(Application.class);

        when(applications.findById(10L))
                .thenReturn(Optional.of(application));

        // Simulate application ownership check failing.
        when(application.belongsTo(applicant)).thenReturn(false);

        // Verify the service rejects access.
        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> service.getApplicationDetails(10L, 1L)
        );

        // Verify the correct error message.
        assertEquals(
                "You can only access your own application.",
                exception.getMessage()
        );

        // Verify application details were never returned.
        verify(application, never()).getDetails();
    }


    @Test
    void getApplicationsByApplicant_InactiveAccount() {

        // Create a mock applicant and account.
        Applicant applicant = mock(Applicant.class);

        // Simulate finding applicant ID 1.
        when(applicants.findById(1L))
                .thenReturn(Optional.of(applicant));


        // Simulate an applicant whose account is deactivated.
        when(applicant.getRole()).thenReturn(Role.APPLICANT);
        when(applicant.getStatus()).thenReturn(AccountStatus.DEACTIVATED);

        // Verify the service rejects the inactive applicant.
        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> service.getApplicationsByApplicant(1L)
        );

        // Verify the error message.
        assertEquals(
                "An active applicant account is required.",
                exception.getMessage()
        );

        // Verify no applications were retrieved.
        verify(applications, never()).findByApplicantId(anyLong());
    }
}
