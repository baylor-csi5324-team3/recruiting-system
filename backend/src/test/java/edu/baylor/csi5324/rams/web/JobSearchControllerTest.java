package edu.baylor.csi5324.rams.web;

import edu.baylor.csi5324.rams.domain.Company;
import edu.baylor.csi5324.rams.domain.JobPosting;
import edu.baylor.csi5324.rams.domain.PostingDetails;
import edu.baylor.csi5324.rams.domain.Recruiter;
import edu.baylor.csi5324.rams.domain.SearchCriteria;
import edu.baylor.csi5324.rams.service.JobPostingService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** UC-01 Search Available Jobs, web layer. */
@WebMvcTest(JobSearchController.class)
class JobSearchControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean JobPostingService jobPostingService;

    private static JobPosting published(String title) {
        Recruiter recruiter = new Recruiter("Rhea", new Company("Baylor Tech"));
        JobPosting posting = new JobPosting(new PostingDetails(title, "desc", "Waco, TX", "FULL_TIME",
                LocalDate.of(2026, 11, 30)), recruiter, LocalDateTime.of(2026, 10, 1, 9, 0));
        posting.markPublished(recruiter, LocalDateTime.of(2026, 10, 2, 9, 0));
        return posting;
    }

    @Test
    @DisplayName("GET /api/jobs/search asks for the criteria (step 2)")
    void asksForCriteria() throws Exception {
        mockMvc.perform(get("/api/jobs/search"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fields", org.hamcrest.Matchers.contains("keyword", "location", "employmentType")));
    }

    @Test
    @DisplayName("GET /api/jobs/search/results passes the criteria on and reports each posting's summary (steps 3 to 7)")
    void returnsSummaries() throws Exception {
        when(jobPostingService.findOpenPostings(any())).thenReturn(List.of(published("Backend Developer")));

        mockMvc.perform(get("/api/jobs/search/results").param("keyword", "backend").param("location", "Waco, TX"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("Backend Developer"))
                .andExpect(jsonPath("$[0].companyName").value("Baylor Tech"))
                .andExpect(jsonPath("$[0].location").value("Waco, TX"))
                .andExpect(jsonPath("$[0].employmentType").value("FULL_TIME"))
                .andExpect(jsonPath("$[0].closingDate").value("2026-11-30"));

        ArgumentCaptor<SearchCriteria> criteria = ArgumentCaptor.forClass(SearchCriteria.class);
        verify(jobPostingService).findOpenPostings(criteria.capture());
        assertEquals(new SearchCriteria("backend", "Waco, TX", null), criteria.getValue());
    }

    @Test
    @DisplayName("no criteria is allowed (extension 3a) and no match gives an empty list (extension 5a)")
    void noCriteriaAndNoMatch() throws Exception {
        when(jobPostingService.findOpenPostings(any())).thenReturn(List.of());

        mockMvc.perform(get("/api/jobs/search/results"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("invalid criteria are reported and the search is not run (extension 4a)")
    void invalidCriteriaReported() throws Exception {
        mockMvc.perform(get("/api/jobs/search/results").param("keyword", "x".repeat(101)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("keyword")));

        verify(jobPostingService, never()).findOpenPostings(any());
    }
}
