package edu.baylor.csi5324.rams.web;

import edu.baylor.csi5324.rams.domain.JobPosting;
import edu.baylor.csi5324.rams.domain.PostingDetails;
import edu.baylor.csi5324.rams.domain.Recruiter;
import edu.baylor.csi5324.rams.repository.RecruiterRepository;
import edu.baylor.csi5324.rams.service.JobPostingService;
import edu.baylor.csi5324.rams.service.NotFoundException;
import edu.baylor.csi5324.rams.service.ValidationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Use case 4.2.2.1 Create and Publish a Job Posting (SD 6.9). Controller lifeline of the sequence diagram:
 * 1 startPosting, 2 submitDetails, 2.1 validate, 2.2 createDraft, 3 publish, 3.1 publish.
 *
 * <p>The recruiter is identified by the {@code X-Recruiter-Id} header (precondition 1 of the use case);
 * IdentityCheckInterceptor makes sure it is the signed-in user.
 */
@RestController
@RequestMapping("/api/postings")
public class JobPostingController {

    public static final String RECRUITER_HEADER = "X-Recruiter-Id";

    private final JobPostingService jobPostingService;
    private final RecruiterRepository recruiters;
    private final Clock clock;

    public JobPostingController(JobPostingService jobPostingService, RecruiterRepository recruiters, Clock clock) {
        this.jobPostingService = jobPostingService;
        this.recruiters = recruiters;
        this.clock = clock;
    }

    /** Steps 1 and 2: the recruiter starts a posting and the system asks for the details. */
    @GetMapping("/new")
    public Prompt startPosting() {
        return new Prompt("Provide the title, description, location, employment type and closing date.",
                List.of("title", "description", "location", "employmentType", "closingDate"));
    }

    /** Steps 3 and 4: validates the details and stores the posting (DRAFT until step 5). */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public JobPosting submitDetails(@RequestBody PostingDetails details,
                                    @RequestHeader(RECRUITER_HEADER) Long recruiterId) {
        Recruiter recruiter = identifiedRecruiter(recruiterId);
        validate(details);                                               // 2.2 (2.1 = identifiedRecruiter)
        return jobPostingService.createDraft(details, recruiter);        // 2.3
    }

    /** Steps 5 to 7: publishes the posting and reports it (SD 3, 3.1 identifies the recruiter). */
    @PostMapping("/{postingId}/publish")
    public JobPosting publish(@PathVariable Long postingId,
                              @RequestHeader(RECRUITER_HEADER) Long recruiterId) {
        Recruiter recruiter = identifiedRecruiter(recruiterId);
        return jobPostingService.publish(postingId, recruiter);          // 3.2
    }

    @PutMapping("/{postingId}")
    public JobPosting edit(@PathVariable Long postingId, @RequestBody PostingDetails details,
                           @RequestHeader(RECRUITER_HEADER) Long recruiterId) {
        validate(details);
        return jobPostingService.updateDetails(postingId, details, identifiedRecruiter(recruiterId));
    }

    @PostMapping("/{postingId}/close")
    public JobPosting close(@PathVariable Long postingId,
                            @RequestHeader(RECRUITER_HEADER) Long recruiterId) {
        return jobPostingService.close(postingId, identifiedRecruiter(recruiterId));
    }

    /** Step 4: extension 4a (missing detail) and 4b (closing date not later than today). */
    private void validate(PostingDetails details) {
        List<String> missing = new ArrayList<>();
        if (isBlank(details.title())) missing.add("title");
        if (isBlank(details.description())) missing.add("description");
        if (isBlank(details.location())) missing.add("location");
        if (isBlank(details.employmentType())) missing.add("employmentType");
        if (details.closingDate() == null) missing.add("closingDate");
        if (!missing.isEmpty()) {
            throw new ValidationException("Missing: " + String.join(", ", missing) + ". Provide the missing detail(s).");
        }
        if (!details.closingDate().isAfter(LocalDate.now(clock))) {
            throw new ValidationException("The closing date must be later than today. Provide a later closing date.");
        }
    }

    private Recruiter identifiedRecruiter(Long recruiterId) {
        return recruiters.findById(recruiterId)
                .orElseThrow(() -> new NotFoundException("No recruiter with id " + recruiterId + "."));
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
