
package edu.baylor.csi5324.rams.web;

import edu.baylor.csi5324.rams.domain.ApplicationStage;
import edu.baylor.csi5324.rams.domain.ApplicationSummary;
import edu.baylor.csi5324.rams.service.ApplicationService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApplicationTrackingControllerTest {

    @Mock
    private ApplicationService applicationService;

    @InjectMocks
    private ApplicationController controller;

    // Test 1: Retrieve all applications for an applicant.
    @Test
    void getApplications_ReturnsApplications() {

        ApplicationSummary summary1 = new ApplicationSummary(
                10L,
                "Software Engineer",
                "Baylor Tech",
                ApplicationStage.SUBMITTED,
                null
        );

        ApplicationSummary summary2 = new ApplicationSummary(
                11L,
                "Backend Developer",
                "Baylor Tech",
                ApplicationStage.INTERVIEW,
                null
        );

        when(applicationService.getApplicationsByApplicant(1L))
                .thenReturn(List.of(summary1, summary2));

        List<ApplicationSummary> result =
                controller.getApplications(1L);

        assertEquals(2, result.size());
        assertEquals(10L, result.get(0).applicationId());
        assertEquals(ApplicationStage.SUBMITTED, result.get(0).stage());
        assertEquals(11L, result.get(1).applicationId());
        assertEquals(ApplicationStage.INTERVIEW, result.get(1).stage());

        verify(applicationService).getApplicationsByApplicant(1L);
    }

    // Test 2: Retrieve details for a selected application.
    @Test
    void getApplicationDetails_ReturnsDetails() {

        ApplicationSummary summary = new ApplicationSummary(
                10L,
                "Software Engineer",
                "Baylor Tech",
                ApplicationStage.UNDER_REVIEW,
                null
        );

        when(applicationService.getApplicationDetails(10L, 1L))
                .thenReturn(summary);

        ApplicationSummary result =
                controller.getApplicationDetails(10L, 1L);

        assertEquals(10L, result.applicationId());
        assertEquals("Software Engineer", result.jobTitle());
        assertEquals("Baylor Tech", result.companyName());
        assertEquals(ApplicationStage.UNDER_REVIEW, result.stage());

        verify(applicationService).getApplicationDetails(10L, 1L);
    }

    // Test 3: Applicant has no submitted applications.
    @Test
    void getApplications_NoApplications() {

        when(applicationService.getApplicationsByApplicant(1L))
                .thenReturn(List.of());

        List<ApplicationSummary> result =
                controller.getApplications(1L);

        assertTrue(result.isEmpty());

        verify(applicationService).getApplicationsByApplicant(1L);
    }
}
