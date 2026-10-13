package edu.baylor.csi5324.rams.web;

import edu.baylor.csi5324.rams.domain.ApplicationSummary;
import edu.baylor.csi5324.rams.service.ApplicationService;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/applications")
public class ApplicationController {

    public static final String APPLICANT_HEADER = "X-Applicant-Id";

    private final ApplicationService applicationService;

    public ApplicationController(ApplicationService applicationService) {
        this.applicationService = applicationService;
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