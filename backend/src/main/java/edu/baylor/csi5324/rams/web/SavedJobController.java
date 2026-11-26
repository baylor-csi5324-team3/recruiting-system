
package edu.baylor.csi5324.rams.web;

import edu.baylor.csi5324.rams.domain.SavedJob;
import edu.baylor.csi5324.rams.service.SavedJobService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/applicants/{applicantId}/saved-jobs")
public class SavedJobController {

    private final SavedJobService savedJobService;
    private final Clock clock;

    public SavedJobController(
            SavedJobService savedJobService,
            Clock clock) {

        this.savedJobService = savedJobService;
        this.clock = clock;
    }

    /**
     * Save Job Posting:
     * create and persist a new saved job.
     */
    @PostMapping
    public ResponseEntity<SavedJobResponse> saveJobPosting(
            @PathVariable Long applicantId,
            @RequestBody SaveJobRequest request) {

        SavedJob savedJob = savedJobService.saveJobPosting(
                applicantId,
                request.jobPostingId());

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(SavedJobResponse.from(
                        savedJob, LocalDate.now(clock)));
    }

    /**
     * Retrieve the applicant's saved jobs,
     * including jobs that have subsequently closed.
     */
    @GetMapping
    public List<SavedJobResponse> listSavedJobs(
            @PathVariable Long applicantId) {

        LocalDate today = LocalDate.now(clock);

        return savedJobService.listSavedJobs(applicantId)
                .stream()
                .map(savedJob ->
                        SavedJobResponse.from(savedJob, today))
                .toList();
    }

    /**
     * Remove a saved job without deleting
     * the original job posting.
     */
    @DeleteMapping("/{jobPostingId}")
    public ResponseEntity<Void> removeSavedJob(
            @PathVariable Long applicantId,
            @PathVariable Long jobPostingId) {

        savedJobService.removeSavedJob(
                applicantId, jobPostingId);

        return ResponseEntity.noContent().build();
    }
}
