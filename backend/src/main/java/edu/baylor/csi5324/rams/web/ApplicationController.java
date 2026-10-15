package edu.baylor.csi5324.rams.web;

import edu.baylor.csi5324.rams.domain.ApplicationSummary;
import edu.baylor.csi5324.rams.service.ApplicationService;

import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import java.util.List;

@RestController
@RequestMapping("/api/applications")
public class ApplicationController {

    public static final String APPLICANT_HEADER = "X-Applicant-Id";

    private final ApplicationService applicationService;

    public ApplicationController(ApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    // Retrieve all applications for an applicant.
    @GetMapping("/tracking")
    public List<ApplicationSummary> getApplications(
            @RequestHeader(APPLICANT_HEADER) Long applicantId) {

        return applicationService.getApplicationsByApplicant(applicantId);
    }

    // Retrieve a selected application's details.
    @GetMapping("/{applicationId}")
    public ApplicationSummary getApplicationDetails(
            @PathVariable Long applicationId,
            @RequestHeader(APPLICANT_HEADER) Long applicantId) {

        return applicationService.getApplicationDetails(
                applicationId,
                applicantId
        );
    }

    /** Submit Job Application: POST persists a new record. */
    @PostMapping
    public ResponseEntity<SubmittedApplicationResponse> submit(
            @Valid @RequestBody SubmitApplicationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(applicationService.submitApplication(request));
    }

    /** My Applications: GET re-reads records from the database. */
    @GetMapping
    public List<SubmittedApplicationResponse> list(@RequestParam Long applicantId) {
        return applicationService.listApplications(applicantId);
    }

    // Retrieve all applications for an applicant.
    @GetMapping
    public List<ApplicationSummary> getApplications(
            @RequestHeader(APPLICANT_HEADER) Long applicantId) {

        return applicationService.getApplicationsByApplicant(applicantId);
    }

    // Retrieve a selected application's details.
    @GetMapping("/{applicationId}")
    public ApplicationSummary getApplicationDetails(
            @PathVariable Long applicationId,
            @RequestHeader(APPLICANT_HEADER) Long applicantId) {

        return applicationService.getApplicationDetails(
                applicationId,
                applicantId
        );
    }

    @GetMapping("/{applicationId}/offer")
    public ApplicationSummary viewOffer(
            @PathVariable Long applicationId,
            @RequestHeader(APPLICANT_HEADER) Long applicantId) {

        return applicationService.getOfferedApplication(
                applicationId,
                applicantId
        );
    }

    @PostMapping("/{applicationId}/offer-response")
    public ApplicationSummary respondToJobOffer(
            @PathVariable Long applicationId,
            @RequestHeader(APPLICANT_HEADER) Long applicantId,
            @RequestBody OfferResponseRequest request) {

        return applicationService.updateStage(
                applicationId,
                applicantId,
                request.decision()
        );
    }
}