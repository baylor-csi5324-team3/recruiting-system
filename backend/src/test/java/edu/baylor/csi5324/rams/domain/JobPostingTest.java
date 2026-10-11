package edu.baylor.csi5324.rams.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** UC-01 step 5 and UC-09 steps 5 and 7, checked on the domain object alone. */
class JobPostingTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 12);
    private static final LocalDateTime NOW = TODAY.atTime(9, 0);

    private final Company company = new Company("Baylor Tech");
    private final Recruiter recruiter = new Recruiter("Rhea", company);

    private JobPosting published(String title, String location, String type, LocalDate closing) {
        JobPosting posting = new JobPosting(new PostingDetails(title, "desc", location, type, closing), recruiter, NOW);
        posting.markPublished(recruiter, NOW);
        return posting;
    }

    @Test
    @DisplayName("a new posting is a DRAFT with no publication time")
    void newPostingIsDraft() {
        JobPosting draft = new JobPosting(new PostingDetails("QA", "desc", "Waco", "full_time", TODAY.plusDays(5)), recruiter, NOW);
        assertEquals(PostingStatus.DRAFT, draft.getStatus());
        assertNull(draft.getPostedDate());
        assertEquals("FULL_TIME", draft.getEmploymentType());
        assertEquals(company, draft.getCompany());
    }

    @Test
    @DisplayName("markPublished records PUBLISHED, the recruiter and the publication time")
    void markPublishedRecordsRecruiterAndTime() {
        JobPosting posting = new JobPosting(new PostingDetails("QA", "desc", "Waco", "FULL_TIME", TODAY.plusDays(5)), recruiter, NOW);
        posting.markPublished(recruiter, NOW.plusHours(1));
        assertEquals(PostingStatus.PUBLISHED, posting.getStatus());
        assertEquals(NOW.plusHours(1), posting.getPostedDate());
        assertEquals(recruiter, posting.getRecruiter());
    }

    @Test
    @DisplayName("no criteria matches every open posting (extension 3a)")
    void noCriteriaMatchesOpenPosting() {
        assertTrue(published("Backend Developer", "Waco, TX", "FULL_TIME", TODAY.plusDays(1)).isOpenAndMatches(SearchCriteria.NONE, TODAY));
    }

    @Test
    @DisplayName("keyword matches the title case-insensitively; location and type must match exactly")
    void criteriaMatching() {
        JobPosting posting = published("Backend Developer", "Waco, TX", "FULL_TIME", TODAY.plusDays(1));
        assertTrue(posting.isOpenAndMatches(new SearchCriteria("backend", null, null), TODAY));
        assertTrue(posting.isOpenAndMatches(new SearchCriteria("BACKEND", "waco, tx", "full_time"), TODAY));
        assertFalse(posting.isOpenAndMatches(new SearchCriteria("frontend", null, null), TODAY));
        assertFalse(posting.isOpenAndMatches(new SearchCriteria(null, "Austin, TX", null), TODAY));
        assertFalse(posting.isOpenAndMatches(new SearchCriteria(null, null, "INTERNSHIP"), TODAY));
    }

    @Test
    @DisplayName("a draft or a posting closed before today is never returned")
    void draftsAndClosedPostingsAreNotOpen() {
        JobPosting draft = new JobPosting(new PostingDetails("QA", "desc", "Waco", "FULL_TIME", TODAY.plusDays(5)), recruiter, NOW);
        assertFalse(draft.isOpenAndMatches(SearchCriteria.NONE, TODAY));
        assertFalse(published("Old", "Waco", "FULL_TIME", TODAY.minusDays(1)).isOpenAndMatches(SearchCriteria.NONE, TODAY));
        assertTrue(published("Last day", "Waco", "FULL_TIME", TODAY).isOpenAndMatches(SearchCriteria.NONE, TODAY));
    }

    @Test
    @DisplayName("getSummary reports title, company, location, employment type and closing date (step 7)")
    void summaryUsesCompanyName() {
        PostingSummary summary = published("Backend Developer", "Waco, TX", "FULL_TIME", TODAY.plusDays(1)).getSummary();
        assertEquals("Backend Developer", summary.title());
        assertEquals("Baylor Tech", summary.companyName());
        assertEquals("Waco, TX", summary.location());
        assertEquals("FULL_TIME", summary.employmentType());
        assertEquals(TODAY.plusDays(1), summary.closingDate());
    }
}
