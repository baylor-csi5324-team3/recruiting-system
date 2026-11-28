package edu.baylor.csi5324.rams.web;

import edu.baylor.csi5324.rams.domain.Application;
import edu.baylor.csi5324.rams.domain.ApplicationStage;
import edu.baylor.csi5324.rams.domain.StageRecord;
import java.time.LocalDateTime;
import java.util.List;

/** Candidate pipeline read model, without resume binary content. */
public record RecruiterApplicationResponse(Long id, Long jobId, String name,
        String email, ApplicationStage stage, LocalDateTime submittedAt,
        String resumeFilename, List<StageRecord> history) {
    public static RecruiterApplicationResponse from(Application application) {
        return new RecruiterApplicationResponse(application.getId(),
                application.getJobPosting().getId(), application.getApplicant().getName(),
                application.getApplicant().getEmail(), application.getStage(),
                application.getSubmitDate(), application.getResumeFilename(),
                application.getStageHistory());
    }
}
