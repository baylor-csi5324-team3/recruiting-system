package edu.baylor.csi5324.rams.web;

import edu.baylor.csi5324.rams.domain.Application;
import edu.baylor.csi5324.rams.domain.ApplicationStage;
import java.time.LocalDateTime;

/** Safe summary for submission confirmation and My Applications. */
public record SubmittedApplicationResponse(
        Long applicationId, Long applicantId, Long jobPostingId,
        String jobTitle, String companyName, ApplicationStage stage,
        LocalDateTime submittedAt, String resumeFilename) {
    public static SubmittedApplicationResponse from(Application application) {
        return new SubmittedApplicationResponse(
                application.getId(), application.getApplicant().getId(),
                application.getJobPosting().getId(),
                application.getJobPosting().getTitle(),
                application.getJobPosting().getCompany().getName(),
                application.getStage(), application.getSubmitDate(),
                application.getResumeFilename());
    }
}
