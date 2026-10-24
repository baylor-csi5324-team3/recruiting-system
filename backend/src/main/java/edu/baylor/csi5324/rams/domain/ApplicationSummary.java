package edu.baylor.csi5324.rams.domain;

import java.time.LocalDateTime;

public record ApplicationSummary(
        Long applicationId,
        String jobTitle,
        String companyName,
        ApplicationStage stage,
        LocalDateTime stageUpdatedAt) {

    public static ApplicationSummary from(Application application) {
        return new ApplicationSummary(
                application.getId(),
                application.getJobPosting().getTitle(),
                application.getJobPosting().getCompany().getName(),
                application.getStage(),
                application.getStageUpdatedAt()
        );
    }
}