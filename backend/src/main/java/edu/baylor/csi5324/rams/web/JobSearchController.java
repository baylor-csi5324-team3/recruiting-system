package edu.baylor.csi5324.rams.web;

import edu.baylor.csi5324.rams.domain.JobPosting;
import edu.baylor.csi5324.rams.domain.PostingSummary;
import edu.baylor.csi5324.rams.domain.SearchCriteria;
import edu.baylor.csi5324.rams.domain.SearchFilters;
import edu.baylor.csi5324.rams.service.JobPostingService;
import edu.baylor.csi5324.rams.service.ValidationException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * UC-01 Search Available Jobs. Controller lifeline of the sequence diagram:
 * 1 searchJobs, 2 submitCriteria, 2.1 validate, 2.2 findOpenPostings, 2.3 getSummary.
 * No sign in is needed (Requirements Clarification Q1).
 */
@RestController
@RequestMapping("/api/jobs")
public class JobSearchController {

    private static final int MAX_LENGTH = 100;

    private final JobPostingService jobPostingService;

    public JobSearchController(JobPostingService jobPostingService) {
        this.jobPostingService = jobPostingService;
    }

    /** Step 1 and 2: the applicant starts a search and the system asks for the criteria. */
    @GetMapping("/search")
    public Prompt searchJobs() {
        return new Prompt("Provide a job title keyword, other criteria, or both. All are optional.",
                List.of("keyword", "location", "employmentType"));
    }

    /** Steps 3 to 7: matching open postings, most recently posted first. */
    @GetMapping("/search/results")
    public List<PostingSummary> submitCriteria(@RequestParam(required = false) String keyword,
                                               @ModelAttribute SearchFilters filters) {
        SearchCriteria criteria = new SearchCriteria(keyword, filters.location(), filters.employmentType());
        validate(criteria);                                                       // 2.1
        List<JobPosting> matchingPostings = jobPostingService.findOpenPostings(criteria);   // 2.2
        return matchingPostings.stream().map(JobPosting::getSummary).toList();   // 2.3
    }

    /** Step 4 (extension 4a): a criterion that is too long to be a real title, place or type. */
    private void validate(SearchCriteria criteria) {
        if (criteria.hasKeyword() && criteria.keyword().trim().length() > MAX_LENGTH) {
            throw new ValidationException("The keyword is longer than " + MAX_LENGTH + " characters. Shorten it.");
        }
        if (criteria.hasLocation() && criteria.location().trim().length() > MAX_LENGTH) {
            throw new ValidationException("The location is longer than " + MAX_LENGTH + " characters. Shorten it.");
        }
        if (criteria.hasEmploymentType() && criteria.employmentType().trim().length() > MAX_LENGTH) {
            throw new ValidationException("The employment type is longer than " + MAX_LENGTH + " characters. Shorten it.");
        }
    }
}
