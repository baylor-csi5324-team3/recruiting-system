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
        List<JobPosting> matches = jobPostings.findByStatus(PostingStatus.PUBLISHED).stream()
                .filter(posting -> posting.isOpenAndMatches(criteria, today))   // 2.2.1
                .toList();
        return sortByPostedDate(matches);                                        // 2.2.2
    }

    private List<JobPosting> sortByPostedDate(List<JobPosting> matches) {
        return matches.stream()
                .sorted(Comparator.comparing(JobPosting::getPostedDate).reversed())
                .toList();
    }

    /** SD 2.2: saves the posting as a draft, recording the recruiter and the creation time. */
    public JobPosting createDraft(PostingDetails details, Recruiter recruiter) {
        JobPosting draft = new JobPosting(details, recruiter, LocalDateTime.now(clock));   // 2.2.1 «create»
        return jobPostings.save(draft);
    }

    /** SD 3.1: records the draft as published with the recruiter and the publication time. */
    public JobPosting publish(Long postingId, Recruiter recruiter) {
        JobPosting posting = jobPostings.findById(postingId)
                .orElseThrow(() -> new NotFoundException("No posting with id " + postingId + "."));
        if (!posting.belongsTo(recruiter.getCompany())) {
            throw new ValidationException("The posting belongs to another company.");
        }
        if (posting.getStatus() != PostingStatus.DRAFT) {
            throw new ValidationException("Only a draft can be published; this posting is " + posting.getStatus() + ".");
        }
        if (!posting.getClosingDate().isAfter(LocalDate.now(clock))) {      // extension 6a
            throw new ValidationException("The closing date has passed while the posting was a draft. Provide a new closing date.");
        }
        posting.markPublished(recruiter, LocalDateTime.now(clock));          // 3.1.1
        return jobPostings.save(posting);
    }
}
