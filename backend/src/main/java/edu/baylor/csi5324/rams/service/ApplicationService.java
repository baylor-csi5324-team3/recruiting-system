package edu.baylor.csi5324.rams.service;

import edu.baylor.csi5324.rams.domain.AccountStatus;
import edu.baylor.csi5324.rams.domain.Applicant;
import edu.baylor.csi5324.rams.domain.Application;
import edu.baylor.csi5324.rams.domain.ApplicationStage;
import edu.baylor.csi5324.rams.domain.ApplicationSummary;
import edu.baylor.csi5324.rams.domain.Role;
import edu.baylor.csi5324.rams.domain.JobPosting;
import edu.baylor.csi5324.rams.domain.PostingStatus;
import edu.baylor.csi5324.rams.domain.Resume;
import edu.baylor.csi5324.rams.repository.JobPostingRepository;
import edu.baylor.csi5324.rams.repository.ResumeRepository;
import edu.baylor.csi5324.rams.web.SubmitApplicationRequest;
import edu.baylor.csi5324.rams.web.SubmittedApplicationResponse;
import java.time.LocalDate;
import java.util.List;
import edu.baylor.csi5324.rams.domain.UserAccount;
import edu.baylor.csi5324.rams.repository.ApplicantRepository;
import edu.baylor.csi5324.rams.repository.ApplicationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import edu.baylor.csi5324.rams.domain.Recruiter;
import edu.baylor.csi5324.rams.repository.RecruiterRepository;
import java.time.Clock;
import java.time.LocalDateTime;

@Service
@Transactional
public class ApplicationService {

    private final ApplicationRepository applications;
    private final ApplicantRepository applicants;
    private final Clock clock;
    private final JobPostingRepository jobs;
    private final ResumeRepository resumes;
    private final RecruiterRepository recruiters;

    public ApplicationService(
            ApplicationRepository applications,
            ApplicantRepository applicants,
            Clock clock,
            JobPostingRepository jobs,
            ResumeRepository resumes,
            RecruiterRepository recruiters) {

        this.applications = applications;
        this.applicants = applicants;
        this.clock = clock;
        this.jobs = jobs;
        this.resumes = resumes;
        this.recruiters = recruiters;
    }

    /**
     * Submit Job Application: validate ownership and materials (step 4),
     * check job availability and duplicates (extensions), and persist a
     * SUBMITTED application with an immutable resume snapshot (steps 7-9).
     */
    public SubmittedApplicationResponse submitApplication(SubmitApplicationRequest request) {
        // Step 4: only active applicants can submit applications.
        Applicant applicant = requireActiveApplicant(request.applicantId());

        // Step 4: selected resume must exist, be nonempty, and belong to applicant.
        Resume resume = resumes.findById(request.resumeId())
                .orElseThrow(() -> new NotFoundException("Resume not found."));
        if (!resume.getApplicant().getId().equals(applicant.getId())) {
            throw new ValidationException("Resume does not belong to this applicant.");
        }
        if (resume.getContent().length == 0) {
            throw new ValidationException("Resume cannot be empty.");
        }

        // Step 4 / closed-job extension: recheck availability at confirmation.
        JobPosting job = jobs.findById(request.jobPostingId())
                .orElseThrow(() -> new NotFoundException("Job posting not found."));
        if (job.getStatus() != PostingStatus.PUBLISHED
                || job.getClosingDate().isBefore(LocalDate.now(clock))) {
            throw new ValidationException("Job posting is not open for applications.");
        }

        // Duplicate extension: one application per applicant and job.
        if (applications.existsByApplicantIdAndJobPostingId(applicant.getId(), job.getId())) {
            throw new ValidationException("You have already applied to this job.");
        }

        // Steps 7-9: save the snapshot and return the persisted record.
        Application application = Application.submittedWithMaterials(
                applicant, job, LocalDateTime.now(clock), resume, request.coverLetter());
        return SubmittedApplicationResponse.from(applications.saveAndFlush(application));
    }

    /** My Applications reads persisted applications rather than browser state. */
    @Transactional(readOnly = true)
    public List<SubmittedApplicationResponse> listApplications(Long applicantId) {
        requireActiveApplicant(applicantId);
        return applications.findByApplicantIdOrderBySubmitDateDesc(applicantId)
                .stream().map(SubmittedApplicationResponse::from).toList();
    }

    private Applicant requireActiveApplicant(Long applicantId) {
        Applicant applicant = applicants.findById(applicantId)
                .orElseThrow(() -> new NotFoundException("Applicant not found."));
        UserAccount account = applicant;
        if (account == null || account.getRole() != Role.APPLICANT
                || account.getStatus() != AccountStatus.ACTIVE) {
            throw new ValidationException("An active applicant account is required.");
        }
        return applicant;
    }

    /** Submitted resume snapshot, available to an active recruiter from the job's company. */
    public record SubmittedResume(String filename, byte[] content) {}

    @Transactional(readOnly = true)
    public SubmittedResume getSubmittedResumeForRecruiter(Long applicationId, Long recruiterId) {
        Recruiter recruiter = recruiters.findById(recruiterId)
                .orElseThrow(() -> new NotFoundException("Recruiter not found."));
        UserAccount account = recruiter;
        if (account == null || account.getRole() != Role.RECRUITER
                || account.getStatus() != AccountStatus.ACTIVE) {
            throw new ValidationException("An active recruiter account is required.");
        }
        Application application = applications.findById(applicationId)
                .orElseThrow(() -> new NotFoundException("Application not found."));
        if (recruiter.getCompany() == null
                || !application.getJobPosting().belongsTo(recruiter.getCompany())) {
            throw new ValidationException("The application belongs to another company.");
        }
        byte[] content = application.getSubmittedResume();
        if (content == null || content.length == 0) {
            throw new NotFoundException("This application has no submitted resume file.");
        }
        String filename = application.getResumeFilename();
        return new SubmittedResume(
                filename == null || filename.isBlank() ? "resume" : filename,
                content);
    }

    // Track application stage
    // Retrieve all applications belonging to the applicant
    @Transactional(readOnly = true)
    public List<ApplicationSummary> getApplicationsByApplicant(
            Long applicantId) {

        // Verify that the applicant exists and has an active account.
        validateApplicant(applicantId);

        // Retrieve the applicant's applications and return their summaries.
        return applications.findByApplicantId(applicantId)
                .stream()
                .map(Application::getDetails)
                .toList();
    }

    // Retrieve the details of a selected application.
    @Transactional(readOnly = true)
    public ApplicationSummary getApplicationDetails(
            Long applicationId,
            Long applicantId) {

        // Use the existing method to verify ownership.
        Application application =
                findOwnedApplication(applicationId, applicantId);

        return application.getDetails();
    }

    public ApplicationSummary withdrawApplication(
            Long applicationId,
            Long applicantId) {

        if (applicationId == null || applicantId == null) {
            throw new ValidationException(
                    "Application ID and Applicant ID are required."
            );
        }

        Application application =
                findOwnedApplication(applicationId, applicantId);

        Long changedByUserId =
                application.getApplicant().getId();

        try {
            application.withdraw(
                    LocalDateTime.now(clock),
                    changedByUserId
            );
        } catch (IllegalArgumentException | IllegalStateException e) {
            throw new ValidationException(e.getMessage());
        }

        return applications.saveAndFlush(application).getDetails();
    }

    public ApplicationSummary extendJobOffer(
            Long applicationId,
            Long recruiterId) {

        return updateStage(
                applicationId,
                recruiterId,
                ApplicationStage.OFFER
        );
    }

    /**
     * Update Application Stage.
     * Only active recruiters from the same company
     * can update the application stage.
     */
    public ApplicationSummary updateStage(
            Long applicationId,
            Long recruiterId,
            ApplicationStage nextStage) {


        // Step 1: Validate requested stage.
        if (nextStage == null) {
            throw new ValidationException(
                    "Application stage is required.");
        }
        if (applicationId == null || recruiterId == null) {
            throw new ValidationException(
                    "Application ID and Recruiter ID are required.");
        }
        // Step 2: Find the recruiter.
        Recruiter recruiter = recruiters.findById(recruiterId)
                .orElseThrow(() ->
                        new NotFoundException("Recruiter not found."));

        // Step 3: Validate recruiter role and account status.
        UserAccount account = recruiter;

        if (account == null
                || account.getRole() != Role.RECRUITER
                || account.getStatus() != AccountStatus.ACTIVE) {

            throw new ValidationException(
                    "An active recruiter account is required.");
        }

        // Step 4: Find the application.
        Application application = applications.findById(applicationId)
                .orElseThrow(() ->
                        new NotFoundException("Application not found."));

        // Step 5: Verify recruiter belongs to the same company.
        if (recruiter.getCompany() == null
                || !application.getJobPosting()
                .belongsTo(recruiter.getCompany())) {

            throw new ValidationException(
                    "The application belongs to another company.");
        }

        // Step 6: Update application stage.
        try {
            application.updateStage(
                    nextStage,
                    LocalDateTime.now(clock),
                    account.getId());

        } catch (IllegalArgumentException | IllegalStateException e) {
            throw new ValidationException(e.getMessage());
        }

        // Step 7: Save the updated application.
        return applications.saveAndFlush(application).getDetails();
    }

    // Loads the application and checks the account status and that it belongs to the applicant.
    private Application findOwnedApplication(
            Long applicationId,
            Long applicantId) {

        Applicant applicant = applicants.findById(applicantId)
                .orElseThrow(() -> new NotFoundException(
                        "No applicant with id " + applicantId + "."
                ));

        UserAccount account = applicant;

        if (account == null
                || account.getRole() != Role.APPLICANT
                || account.getStatus() != AccountStatus.ACTIVE) {
            throw new ValidationException(
                    "An active applicant account is required."
            );
        }

        Application application = applications.findById(applicationId)
                .orElseThrow(() -> new NotFoundException(
                        "No application with id " + applicationId + "."
                ));

        if (!application.belongsTo(applicant)) {
            throw new ValidationException(
                    "You can only access your own application."
            );
        }

        return application;
    }

    // Verify an applicant exists and has an active account.
    private Applicant validateApplicant(Long applicantId) {

        Applicant applicant = applicants.findById(applicantId)
                .orElseThrow(() -> new NotFoundException(
                        "No applicant with id " + applicantId + "."
                ));

        UserAccount account = applicant;

        if (account == null
                || account.getRole() != Role.APPLICANT
                || account.getStatus() != AccountStatus.ACTIVE) {
            throw new ValidationException(
                    "An active applicant account is required."
            );
        }

        return applicant;
    }
}