package edu.baylor.csi5324.rams.domain;

import java.time.LocalDateTime;
import java.util.List;

public record ApplicationSummary(
        Long applicationId,
        String jobTitle,
        String companyName,
        ApplicationStage stage,
        LocalDateTime respondedAt,
        List<StageRecord> stageHistory
) {

    public ApplicationSummary {
        stageHistory = stageHistory == null
                ? List.of()
                : List.copyOf(stageHistory);
    }

    // Keeps existing five-argument calls and tests working.
    public ApplicationSummary(
            Long applicationId,
            String jobTitle,
            String companyName,
            ApplicationStage stage,
            LocalDateTime respondedAt) {

        this(
                applicationId,
                jobTitle,
                companyName,
                stage,
                respondedAt,
                List.of()
        );
    }

    public static ApplicationSummary from(Application application) {
        return new ApplicationSummary(
                application.getId(),
                application.getJobPosting().getTitle(),
                application.getJobPosting().getCompany().getName(),
                application.getStage(),
                application.getRespondedAt(),
                application.getStageHistory()
        );
    }
}