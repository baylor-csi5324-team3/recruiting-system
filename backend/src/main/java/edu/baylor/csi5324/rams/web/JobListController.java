package edu.baylor.csi5324.rams.web;

import edu.baylor.csi5324.rams.domain.JobPosting;
import edu.baylor.csi5324.rams.domain.PostingStatus;
import edu.baylor.csi5324.rams.domain.Recruiter;
import edu.baylor.csi5324.rams.repository.JobPostingRepository;
import edu.baylor.csi5324.rams.repository.RecruiterRepository;
import edu.baylor.csi5324.rams.service.NotFoundException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * The list of job postings shown on the Explore and Job Postings pages (Search Available Jobs,
 * Create and Publish a Job Posting). Everyone sees published and closed postings. A recruiter who names
 * themselves with {@code recruiterId} also sees the drafts of their own company;
 * IdentityCheckInterceptor makes sure that id is the signed-in recruiter.
 */
@RestController
@RequestMapping("/api/jobs")
@Transactional(readOnly = true)
public class JobListController {

    private final JobPostingRepository postings;
    private final RecruiterRepository recruiters;

    public JobListController(JobPostingRepository postings, RecruiterRepository recruiters) {
        this.postings = postings;
        this.recruiters = recruiters;
    }

    /** One posting as the front end shows it. */
    public record JobView(Long id, String title, String description, String qualifications, String companyName,
                          String location, String employmentType, LocalDate closingDate,
                          LocalDateTime postedDate, String status) {
        static JobView of(JobPosting p) {
            return new JobView(p.getId(), p.getTitle(), p.getDescription(), p.getQualifications(),
                    p.getCompany().getName(), p.getLocation(), p.getEmploymentType(), p.getClosingDate(),
                    p.getPostedDate(), p.getStatus().name());
        }
    }

    @GetMapping
    public List<JobView> list(@RequestParam(required = false) Long recruiterId) {
        Recruiter recruiter = recruiterId == null ? null : recruiters.findById(recruiterId)
                .orElseThrow(() -> new NotFoundException("No recruiter with id " + recruiterId + "."));
        return postings.findAll().stream()
                .filter(p -> p.getStatus() != PostingStatus.DRAFT
                        || (recruiter != null && p.belongsTo(recruiter.getCompany())))
                .map(JobView::of)
                .toList();
    }
}
