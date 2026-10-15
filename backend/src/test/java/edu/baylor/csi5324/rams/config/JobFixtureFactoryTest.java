package edu.baylor.csi5324.rams.config;

import edu.baylor.csi5324.rams.domain.*;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.*;

/** Fixture construction only; account resolution, CSV parsing and persistence remain blocked. */
class JobFixtureFactoryTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 12, 9, 0);
    private final Clock clock = Clock.fixed(NOW.atZone(ZoneId.of("America/Chicago")).toInstant(), ZoneId.of("America/Chicago"));
    private final JobFixtureFactory factory = new JobFixtureFactory(clock);
    private final Company company = new Company("Baylor Tech");
    private final Recruiter recruiter = new Recruiter("Rhea Recruiter", company);

    private PostingDetails details(LocalDate closingDate) {
        return new PostingDetails("Backend Developer", "Build services", "Waco, TX", "FULL_TIME", closingDate);
    }

    @Test
    void draftHasCreationTimeButNoPublicationTime() {
        JobPosting posting = factory.create(details(LocalDate.of(2026, 12, 31)), PostingStatus.DRAFT, recruiter, 5);
        assertEquals(PostingStatus.DRAFT, posting.getStatus());
        assertEquals(NOW, posting.getCreatedAt());
        assertNull(posting.getPostedDate());
        assertNull(posting.getId(), "Only persistence may generate an ID");
        assertFalse(posting.isOpenAndMatches(SearchCriteria.NONE, NOW.toLocalDate()));
    }

    @Test
    void publishedFixturePreservesDetailsAndRelationships() {
        JobPosting posting = factory.create(details(LocalDate.of(2026, 12, 31)), PostingStatus.PUBLISHED, recruiter, 2);
        assertEquals("Backend Developer", posting.getTitle());
        assertEquals("Build services", posting.getDescription());
        assertEquals("Waco, TX", posting.getLocation());
        assertEquals("FULL_TIME", posting.getEmploymentType());
        assertSame(recruiter, posting.getRecruiter());
        assertSame(company, posting.getCompany());
        assertEquals(NOW.minusHours(2), posting.getPostedDate());
        assertEquals(NOW.minusHours(3), posting.getCreatedAt());
        assertTrue(posting.isOpenAndMatches(SearchCriteria.NONE, NOW.toLocalDate()));
    }

    @Test
    void historicalFixtureWasCreatedAndPublishedBeforeItsClosingDay() {
        LocalDate closing = LocalDate.of(2026, 10, 5);
        JobPosting posting = factory.create(details(closing), PostingStatus.PUBLISHED, recruiter, 4);
        assertEquals(PostingStatus.PUBLISHED, posting.getStatus());
        assertEquals(closing.atStartOfDay().minusHours(4), posting.getPostedDate());
        assertTrue(posting.getCreatedAt().isBefore(posting.getPostedDate()));
        assertTrue(posting.getPostedDate().isBefore(closing.atStartOfDay()));
        assertFalse(posting.isOpenAndMatches(SearchCriteria.NONE, NOW.toLocalDate()));
    }

    @Test
    void earlierOpenRowsSortBeforeLaterRows() {
        JobPosting first = factory.create(details(LocalDate.of(2026, 12, 31)), PostingStatus.PUBLISHED, recruiter, 1);
        JobPosting second = factory.create(details(LocalDate.of(2026, 12, 31)), PostingStatus.PUBLISHED, recruiter, 2);
        assertTrue(first.getPostedDate().isAfter(second.getPostedDate()));
        assertTrue(first.getPostedDate().isBefore(NOW));
    }

    @Test
    void closingTodayStillHasHistoricalPublicationTime() {
        JobPosting posting = factory.create(details(NOW.toLocalDate()), PostingStatus.PUBLISHED, recruiter, 1);
        assertTrue(posting.getPostedDate().isBefore(NOW.toLocalDate().atStartOfDay()));
        assertTrue(posting.isOpenAndMatches(SearchCriteria.NONE, NOW.toLocalDate()));
    }

    @Test
    void rejectsUnsupportedStatusAndRowNumber() {
        assertThrows(IllegalArgumentException.class, () -> factory.create(details(NOW.toLocalDate()), PostingStatus.CLOSED, recruiter, 1));
        assertThrows(IllegalArgumentException.class, () -> factory.create(details(NOW.toLocalDate()), null, recruiter, 1));
        assertThrows(IllegalArgumentException.class, () -> factory.create(details(NOW.toLocalDate()), PostingStatus.PUBLISHED, recruiter, 0));
    }
}
