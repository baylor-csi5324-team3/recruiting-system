package edu.baylor.csi5324.rams.web;

import edu.baylor.csi5324.rams.domain.ApplicationSummary;
import edu.baylor.csi5324.rams.service.ApplicationService;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/applications")
public class RespondToJobOfferController {

    // 开发期模拟申请人身份，之后替换成真实登录身份。
    public static final String APPLICANT_HEADER = "X-Applicant-Id";

    private final ApplicationService applicationService;

    public RespondToJobOfferController(
            ApplicationService applicationService) {

        this.applicationService = applicationService;
    }

    // 查看申请及当前 Stage。
    @GetMapping("/{applicationId}/offer")
    public ApplicationSummary viewOffer(
            @PathVariable Long applicationId,
            @RequestHeader(APPLICANT_HEADER) Long applicantId) {

        return applicationService.getApplication(
                applicationId,
                applicantId
        );
    }

    // 只有在 Applicant 最终确认后，前端才发送这个请求。
    @PostMapping("/{applicationId}/offer-response")
    public ApplicationSummary respondToOffer(
            @PathVariable Long applicationId,
            @RequestHeader(APPLICANT_HEADER) Long applicantId,
            @RequestBody OfferResponseRequest request) {

        return applicationService.respondToOffer(
                applicationId,
                applicantId,
                request.decision()
        );
    }
}