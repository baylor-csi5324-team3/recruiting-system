package edu.baylor.csi5324.rams.config;

import edu.baylor.csi5324.rams.domain.JobPosting;
import edu.baylor.csi5324.rams.domain.PostingDetails;
import edu.baylor.csi5324.rams.domain.PostingStatus;
import edu.baylor.csi5324.rams.domain.Recruiter;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * Builds already-validated development fixtures; does not parse CSV, resolve accounts or persist data.
 * JobCsvLoader validates each row and resolves its account-linked recruiter first.
 */
public class JobFixtureFactory {

    private final Clock clock;

    public JobFixtureFactory(Clock clock) {
        this.clock = clock;
    }

    /**
     * Data rows are numbered from one (excluding the header). Earlier rows are newer when their
     * closing dates permit it. Publication is before both initialization and the closing day;
     * creation precedes publication by one hour. Drafts are created at initialization time.
     */
    public JobPosting create(PostingDetails details, PostingStatus status, Recruiter recruiter, int rowNumber) {
        if (rowNumber < 1) {
            throw new IllegalArgumentException("The data row number must be positive.");
        }
        if (status != PostingStatus.DRAFT && status != PostingStatus.PUBLISHED) {
            throw new IllegalArgumentException("Sample fixtures support DRAFT and PUBLISHED only.");
        }
        LocalDateTime now = LocalDateTime.now(clock);
        if (status == PostingStatus.DRAFT) {
            return new JobPosting(details, recruiter, now);
        }
        LocalDateTime closingDay = details.closingDate().atStartOfDay();
        LocalDateTime baseline = now.isBefore(closingDay) ? now : closingDay;
        LocalDateTime postedAt = baseline.minusHours(rowNumber);
        JobPosting posting = new JobPosting(details, recruiter, postedAt.minusHours(1));
        posting.markPublished(recruiter, postedAt);
        return posting;
    }
}
