
package edu.baylor.csi5324.rams.web;

import edu.baylor.csi5324.rams.domain.ApplicationStage;
import edu.baylor.csi5324.rams.domain.ApplicationSummary;
import edu.baylor.csi5324.rams.service.ApplicationService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ApplicationController.class)
class UpdateApplicationStageControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private ApplicationService service;

    /**
     * Test 1: A valid recruiter request updates the stage.
     */
    @Test
    void recruiterSuccessfullyUpdatesStage() throws Exception {

        ApplicationSummary summary = new ApplicationSummary(
                15L,
                "Software Engineer",
                "Baylor Tech",
                ApplicationStage.UNDER_REVIEW,
                LocalDateTime.of(2026, 10, 12, 10, 0)
        );

        when(service.updateStage(
                15L, 7L, ApplicationStage.UNDER_REVIEW))
                .thenReturn(summary);

        mvc.perform(patch("/api/applications/15/stage")
                        .header("X-Recruiter-Id", "7")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"stage\":\"UNDER_REVIEW\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.applicationId").value(15))
                .andExpect(jsonPath("$.stage").value("UNDER_REVIEW"));

        verify(service).updateStage(
                15L, 7L, ApplicationStage.UNDER_REVIEW);
    }

    /**
     * Test 2: Missing recruiter header is rejected.
     */
    @Test
    void rejectsMissingRecruiterHeader() throws Exception {

        mvc.perform(patch("/api/applications/15/stage")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"stage\":\"OFFER\"}"))
                .andExpect(status().isBadRequest());

        verify(service, never())
                .updateStage(any(), any(), any());
    }

    /**
     * Test 3: Missing application stage is rejected.
     */
    @Test
    void rejectsMissingStage() throws Exception {

        mvc.perform(patch("/api/applications/15/stage")
                        .header("X-Recruiter-Id", "7")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        verify(service, never())
                .updateStage(any(), any(), any());
    }

    /**
     * Test 4: Unknown application stage is rejected.
     */
    @Test
    void rejectsInvalidStage() throws Exception {

        mvc.perform(patch("/api/applications/15/stage")
                        .header("X-Recruiter-Id", "7")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"stage\":\"ONBOARDED\"}"))
                .andExpect(status().isBadRequest());

        verify(service, never())
                .updateStage(any(), any(), any());
    }

    /**
     * Test 5: The previous Offer Response API no longer exists.
     */
    @Test
    void oldOfferResponseEndpointIsRemoved() throws Exception {

        mvc.perform(post("/api/applications/15/offer-response")
                        .header("X-Applicant-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"decision\":\"ACCEPT\"}"))
                .andExpect(status().isNotFound());

        verify(service, never())
                .updateStage(any(), any(), any());
    }
}
