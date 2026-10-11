package edu.baylor.csi5324.rams.web;

import edu.baylor.csi5324.rams.domain.Company;
import edu.baylor.csi5324.rams.domain.JobPosting;
import edu.baylor.csi5324.rams.domain.PostingDetails;
import edu.baylor.csi5324.rams.domain.Recruiter;
import edu.baylor.csi5324.rams.repository.RecruiterRepository;
import edu.baylor.csi5324.rams.service.JobPostingService;
import edu.baylor.csi5324.rams.service.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** UC-09 Create and Publish a Job Posting, web layer. */
@WebMvcTest(JobPostingController.class)
@Import(JobPostingControllerTest.ClockStub.class)
class JobPostingControllerTest {

    static final LocalDate TODAY = LocalDate.of(2026, 10, 12);

    @TestConfiguration
    static class ClockStub {
        @Bean
        Clock clock() {
            return Clock.fixed(TODAY.atStartOfDay(ZoneId.of("America/Chicago")).toInstant(), ZoneId.of("America/Chicago"));
        }
    }

    @Autowired MockMvc mockMvc;
    @MockitoBean JobPostingService jobPostingService;
    @MockitoBean RecruiterRepository recruiters;

    private final Recruiter recruiter = new Recruiter("Rhea", new Company("Baylor Tech"));

    @BeforeEach
    void recruiterExists() {
        when(recruiters.findById(7L)).thenReturn(Optional.of(recruiter));
        when(recruiters.findById(99L)).thenReturn(Optional.empty());
    }

    private static final String VALID_BODY = """
            {"title": "QA Engineer", "description": "Test the platform", "location": "Waco, TX",
             "employmentType": "FULL_TIME", "closingDate": "2026-11-30"}
            """;

    @Test
    @DisplayName("GET /api/postings/new asks for the posting details (step 2)")
    void asksForDetails() throws Exception {
        mockMvc.perform(get("/api/postings/new"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fields.length()").value(5));
    }

    @Test
    @DisplayName("POST /api/postings validates the details and saves a draft (steps 3 to 5)")
    void savesDraft() throws Exception {
        JobPosting draft = new JobPosting(new PostingDetails("QA Engineer", "Test the platform", "Waco, TX", "FULL_TIME",
                LocalDate.of(2026, 11, 30)), recruiter, LocalDateTime.of(2026, 10, 12, 9, 0));
        when(jobPostingService.createDraft(any(), eq(recruiter))).thenReturn(draft);

        mockMvc.perform(post("/api/postings").header(JobPostingController.RECRUITER_HEADER, "7")
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.title").value("QA Engineer"))
                .andExpect(jsonPath("$.company.name").value("Baylor Tech"));
    }

    @Test
    @DisplayName("a missing detail is named and nothing is saved (extension 4a)")
    void missingDetailReported() throws Exception {
        mockMvc.perform(post("/api/postings").header(JobPostingController.RECRUITER_HEADER, "7")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\": \"QA Engineer\", \"closingDate\": \"2026-11-30\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("description, location, employmentType")));

        verify(jobPostingService, never()).createDraft(any(), any());
    }

    @Test
    @DisplayName("a closing date not later than today is rejected (extension 4b)")
    void closingDateMustBeLater() throws Exception {
        mockMvc.perform(post("/api/postings").header(JobPostingController.RECRUITER_HEADER, "7")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY.replace("2026-11-30", "2026-10-12")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("closing date")));

        verify(jobPostingService, never()).createDraft(any(), any());
    }

    @Test
    @DisplayName("POST /api/postings/{id}/publish reports the published posting (steps 6 to 8)")
    void publishes() throws Exception {
        JobPosting posting = new JobPosting(new PostingDetails("QA Engineer", "Test the platform", "Waco, TX", "FULL_TIME",
                LocalDate.of(2026, 11, 30)), recruiter, LocalDateTime.of(2026, 10, 12, 9, 0));
        posting.markPublished(recruiter, LocalDateTime.of(2026, 10, 12, 10, 0));
        when(jobPostingService.publish(5L, recruiter)).thenReturn(posting);

        mockMvc.perform(post("/api/postings/5/publish").header(JobPostingController.RECRUITER_HEADER, "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLISHED"))
                .andExpect(jsonPath("$.postedDate").value("2026-10-12T10:00:00"))
                .andExpect(jsonPath("$.recruiter.name").value("Rhea"));
    }

    @Test
    @DisplayName("a business-rule failure during publication is reported as 400 (extension 6a)")
    void publishProblemReported() throws Exception {
        when(jobPostingService.publish(5L, recruiter)).thenThrow(new ValidationException("The closing date has passed while the posting was a draft. Provide a new closing date."));

        mockMvc.perform(post("/api/postings/5/publish").header(JobPostingController.RECRUITER_HEADER, "7"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("new closing date")));
    }

    @Test
    @DisplayName("an unknown recruiter is reported (precondition 1)")
    void unknownRecruiter() throws Exception {
        mockMvc.perform(post("/api/postings").header(JobPostingController.RECRUITER_HEADER, "99")
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isNotFound());
    }
}
