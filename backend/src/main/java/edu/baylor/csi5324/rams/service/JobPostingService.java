package edu.baylor.csi5324.rams.service;

import edu.baylor.csi5324.rams.domain.JobPosting;
import edu.baylor.csi5324.rams.domain.PostingDetails;
import edu.baylor.csi5324.rams.domain.PostingStatus;
import edu.baylor.csi5324.rams.domain.Recruiter;
import edu.baylor.csi5324.rams.domain.SearchCriteria;
import edu.baylor.csi5324.rams.repository.JobPostingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

/**
 * Business rules for job postings. Implements the Service lifeline of the sequence diagrams
 * for Search Available Jobs (UC-01) and Create and Publish a Job Posting (UC-09).
 */
@Service
@Transactional
public class JobPostingService {

    private final JobPostingRepository jobPostings;
    private final Clock clock;

    public JobPostingService(JobPostingRepository jobPostings, Clock clock) {
        this.jobPostings = jobPostings;
        this.clock = clock;
    }

    /** SD 2.2: postings that are open and match every criterion, most recently posted first. */
    @Transactional(readOnly = true)
    public List<JobPosting> findOpenPostings(SearchCriteria criteria) {
        LocalDate today = LocalDate.now(clock);
        List<JobPosting> matches = jobPostings.findByStatus(PostingStatus.PUBLISHED).stream()        // 2.2.1
                .filter(posting -> posting.isOpenAndMatches(criteria, today))   // 2.2.2
                .toList();
        return sortByPostedDate(matches);                                        // 2.2.3
    }

    private List<JobPosting> sortByPostedDate(List<JobPosting> matches) {
        return matches.stream()
                .sorted(Comparator.comparing(JobPosting::getPostedDate).reversed())
                .toList();
    }

    /** SD 2.2: saves the posting as a draft, recording the recruiter and the creation time. */
    public JobPosting createDraft(PostingDetails details, Recruiter recruiter) {
        JobPosting draft = new JobPosting(details, recruiter, LocalDateTime.now(clock));   // 2.2.1 «create»
        return jobPostings.save(draft);                                                   // 2.2.2
    }

    public JobPosting updateDetails(Long postingId, PostingDetails details, Recruiter recruiter) {
        JobPosting posting = jobPostings.findById(postingId)
                .orElseThrow(() -> new NotFoundException("No posting with id " + postingId));
        if (!posting.belongsTo(recruiter.getCompany())) throw new ValidationException("The posting belongs to another company.");
        if (details.title() == null || details.title().isBlank() || details.description() == null || details.description().isBlank()
                || details.location() == null || details.location().isBlank() || details.employmentType() == null || details.employmentType().isBlank()
                || details.closingDate() == null) throw new ValidationException("All job details are required.");
        try { posting.updateDetails(details); }
        catch (IllegalStateException e) { throw new ValidationException(e.getMessage()); }
        return jobPostings.save(posting);
    }
    public JobPosting close(Long id, Recruiter recruiter) {
        JobPosting posting = jobPostings.findById(id)
                .orElseThrow(() -> new NotFoundException("No posting with id " + id));
        if (!posting.belongsTo(recruiter.getCompany())) throw new ValidationException("The posting belongs to another company.");
        try { posting.closePosting(); }
        catch (IllegalStateException e) { throw new ValidationException(e.getMessage()); }
        return jobPostings.save(posting);
    }

    /** SD 3.1: loads the draft (3.1.1), checks the company (3.1.2), marks it published (3.1.3) and saves it (3.1.4). */
    public JobPosting publish(Long postingId, Recruiter recruiter) {
        JobPosting posting = jobPostings.findById(postingId)                       // 3.1.1
                .orElseThrow(() -> new NotFoundException("No posting with id " + postingId + "."));
        if (!posting.belongsTo(recruiter.getCompany())) {                          // 3.1.2
            throw new ValidationException("The posting belongs to another company.");
        }
        if (posting.getStatus() != PostingStatus.DRAFT) {
            throw new ValidationException("Only a draft can be published; this posting is " + posting.getStatus() + ".");
        }
        if (!posting.getClosingDate().isAfter(LocalDate.now(clock))) {      // extension 6a
            throw new ValidationException("The closing date has passed while the posting was a draft. Provide a new closing date.");
        }
        posting.markPublished(recruiter, LocalDateTime.now(clock));          // 3.1.3
        return jobPostings.save(posting);                                        // 3.1.4
    }
}
