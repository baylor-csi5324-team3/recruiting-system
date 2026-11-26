
package edu.baylor.csi5324.rams.service;

import edu.baylor.csi5324.rams.domain.AccountStatus;
import edu.baylor.csi5324.rams.domain.Applicant;
import edu.baylor.csi5324.rams.domain.JobPosting;
import edu.baylor.csi5324.rams.domain.PostingStatus;
import edu.baylor.csi5324.rams.domain.Role;
import edu.baylor.csi5324.rams.domain.SavedJob;
import edu.baylor.csi5324.rams.repository.ApplicantRepository;
import edu.baylor.csi5324.rams.repository.JobPostingRepository;
import edu.baylor.csi5324.rams.repository.SavedJobRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class SavedJobService {

    private final SavedJobRepository savedJobs;
    private final ApplicantRepository applicants;
    private final JobPostingRepository jobs;
    private final Clock clock;

    public SavedJobService(
            SavedJobRepository savedJobs,
            ApplicantRepository applicants,
            JobPostingRepository jobs,
            Clock clock) {

        this.savedJobs = savedJobs;
        this.applicants = applicants;
        this.jobs = jobs;
        this.clock = clock;
    }

    /**
     * Save Job Posting: validate the applicant,
     * check availability and duplicates,
     * and persist the saved job.
     */
    public SavedJob saveJobPosting(
            Long applicantId, Long jobPostingId) {

        Applicant applicant =
                requireActiveApplicant(applicantId);

        JobPosting job = getJobPosting(jobPostingId);

        if (job.getStatus() != PostingStatus.PUBLISHED
                || job.getClosingDate().isBefore(LocalDate.now(clock))) {

            throw new ValidationException(
                    "Job posting is not open for saving.");
        }

        verifySavedJobNotAlready(applicantId, jobPostingId);

        return addSavedJob(applicant, job);
    }

    private JobPosting getJobPosting(Long jobPostingId) {

        if (jobPostingId == null) {
            throw new ValidationException(
                    "Job posting ID is required.");
        }

        return jobs.findById(jobPostingId)
                .orElseThrow(() ->
                        new NotFoundException("Job posting not found."));
    }

    private void verifySavedJobNotAlready(
            Long applicantId, Long jobPostingId) {

        if (hasSavedJob(applicantId, jobPostingId)) {
            throw new ValidationException(
                    "You have already saved this job.");
        }
    }

    private boolean hasSavedJob(
            Long applicantId, Long jobPostingId) {

        return savedJobs.existsByApplicantIdAndJobPostingId(
                applicantId, jobPostingId);
    }

    private SavedJob addSavedJob(
            Applicant applicant, JobPosting job) {

        SavedJob savedJob = new SavedJob(
                applicant,
                job,
                LocalDateTime.now(clock));

        return savedJobs.saveAndFlush(savedJob);
    }

    /**
     * Retrieve all saved jobs belonging to the applicant,
     * including postings that have subsequently closed.
     */
    @Transactional(readOnly = true)
    public List<SavedJob> listSavedJobs(Long applicantId) {

        requireActiveApplicant(applicantId);

        return savedJobs
                .findByApplicantIdOrderBySavedAtDesc(applicantId);
    }

    /**
     * Remove a saved job without deleting the job posting.
     */
    public void removeSavedJob(
            Long applicantId, Long jobPostingId) {

        requireActiveApplicant(applicantId);

        if (jobPostingId == null) {
            throw new ValidationException(
                    "Job posting ID is required.");
        }

        savedJobs.deleteByApplicantIdAndJobPostingId(
                applicantId, jobPostingId);
    }

    private Applicant requireActiveApplicant(Long applicantId) {

        if (applicantId == null) {
            throw new ValidationException(
                    "Applicant ID is required.");
        }

        Applicant applicant = applicants.findById(applicantId)
                .orElseThrow(() ->
                        new NotFoundException("Applicant not found."));

        if (applicant.getRole() != Role.APPLICANT
                || applicant.getStatus() != AccountStatus.ACTIVE) {

            throw new ValidationException(
                    "An active applicant account is required.");
        }

        return applicant;
    }
}
