
package edu.baylor.csi5324.rams.web;

import edu.baylor.csi5324.rams.domain.SavedJob;
import edu.baylor.csi5324.rams.domain.JobPosting;
import edu.baylor.csi5324.rams.domain.PostingStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record SavedJobResponse(
        Long savedJobId,
        Long jobPostingId,
        String jobTitle,
        String companyName,
        String location,
        String employmentType,
        String description,
        String qualifications,
        LocalDateTime postedDate,
        PostingStatus status,
        LocalDate closingDate,
        LocalDateTime savedAt,
        boolean open) {

    public static SavedJobResponse from(
            SavedJob savedJob, LocalDate today) {

        JobPosting job = savedJob.getJobPosting();

        boolean open =
                job.getStatus() == PostingStatus.PUBLISHED
                        && !job.getClosingDate().isBefore(today);

        return new SavedJobResponse(
                savedJob.getId(),
                job.getId(),
                job.getTitle(),
                job.getCompany().getName(),
                job.getLocation(),
                job.getEmploymentType(),
                job.getDescription(),
                job.getQualifications(),
                job.getPostedDate(),
                job.getStatus(),
                job.getClosingDate(),
                savedJob.getSavedAt(),
                open);
    }
}
