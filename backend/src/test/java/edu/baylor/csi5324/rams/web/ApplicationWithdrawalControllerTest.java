package edu.baylor.csi5324.rams.web;

import edu.baylor.csi5324.rams.domain.*;
import edu.baylor.csi5324.rams.service.ApplicationService;
import edu.baylor.csi5324.rams.service.NotFoundException;
import edu.baylor.csi5324.rams.service.ValidationException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class ApplicationWithdrawalControllerTest {

    @Mock
    private ApplicationService service;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders
                .standaloneSetup(new ApplicationController(service))
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    @Test
    void withdrawReturnsUpdatedApplicationAndHistory() throws Exception {
        LocalDateTime time =
                LocalDateTime.of(2026, 10, 24, 15, 0);

        ApplicationSummary summary = new ApplicationSummary(
                10L,
                "Backend Developer",
                "Baylor Tech",
                ApplicationStage.WITHDRAWN,
                time,
                List.of(
                        new StageRecord(
                                ApplicationStage.WITHDRAWN,
                                time,
                                101L
                        )
                )
        );

        when(service.withdrawApplication(10L, 1L))
                .thenReturn(summary);

        mvc.perform(post("/api/applications/10/withdraw")
                        .header("X-Applicant-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.applicationId").value(10))
                .andExpect(jsonPath("$.stage").value("WITHDRAWN"))
                .andExpect(jsonPath("$.stageHistory.length()").value(1))
                .andExpect(jsonPath("$.stageHistory[0].stage")
                        .value("WITHDRAWN"))
                .andExpect(jsonPath("$.stageHistory[0].changedAt")
                        .value("2026-10-24T15:00:00"))
                .andExpect(jsonPath("$.stageHistory[0].changedByUserId")
                        .value(101));

        verify(service).withdrawApplication(10L, 1L);
    }

    @Test
    void missingApplicantHeaderReturnsBadRequest() throws Exception {
        mvc.perform(post("/api/applications/10/withdraw"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    void invalidApplicantHeaderReturnsBadRequest() throws Exception {
        mvc.perform(post("/api/applications/10/withdraw")
                        .header("X-Applicant-Id", "invalid"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    void rejectedWithdrawalReturnsBadRequest() throws Exception {
        when(service.withdrawApplication(10L, 1L))
                .thenThrow(new ValidationException(
                        "You can only access your own application."
                ));

        mvc.perform(post("/api/applications/10/withdraw")
                        .header("X-Applicant-Id", "1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("You can only access your own application."));

        verify(service).withdrawApplication(10L, 1L);
    }

    @Test
    void unknownApplicationReturnsNotFound() throws Exception {
        when(service.withdrawApplication(10L, 1L))
                .thenThrow(new NotFoundException(
                        "No application with id 10."
                ));

        mvc.perform(post("/api/applications/10/withdraw")
                        .header("X-Applicant-Id", "1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message")
                        .value("No application with id 10."));

        verify(service).withdrawApplication(10L, 1L);
    }
}