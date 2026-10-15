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

    public ApplicationService(
            ApplicationRepository applications,
            ApplicantRepository applicants,
            Clock clock,
            JobPostingRepository jobs,
            ResumeRepository resumes) {

        this.applications = applications;
        this.applicants = applicants;
        this.clock = clock;
        this.jobs = jobs;
        this.resumes = resumes;
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
        UserAccount account = applicant.getAccount();
        if (account == null || account.getRole() != Role.APPLICANT
                || account.getStatus() != AccountStatus.ACTIVE) {
            throw new ValidationException("An active applicant account is required.");
        }
        return applicant;
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


    // 对应顺序图：getOfferedApplication() → getDetails()。
    @Transactional(readOnly = true)
    public ApplicationSummary getOfferedApplication(
            Long applicationId,
            Long applicantId) {

        Application application =
                findOwnedApplication(applicationId, applicantId);

        return application.getDetails();
    }

    // Applicant 确认接受或拒绝后，更新申请的 Stage。
    public ApplicationSummary updateStage(
            Long applicationId,
            Long applicantId,
            String decision) {

        // 先校验输入，避免将非法值当成 DECLINE。
        if (!"ACCEPT".equals(decision)
                && !"DECLINE".equals(decision)) {
            throw new ValidationException(
                    "Decision must be ACCEPT or DECLINE."
            );
        }

        Application application =
                findOwnedApplication(applicationId, applicantId);

        if (application.getStage() != ApplicationStage.OFFER) {
            throw new ValidationException(
                    "Only an application in OFFER stage can be accepted or declined."
            );
        }

        ApplicationStage nextStage = "ACCEPT".equals(decision)
                ? ApplicationStage.ACCEPTED
                : ApplicationStage.DECLINED;

        // 对应顺序图：recordStage(ACCEPTED / DECLINED)。
        application.recordStage(
                nextStage,
                LocalDateTime.now(clock)
        );

        applications.saveAndFlush(application);

        return application.getDetails();
    }

    // 查询申请，同时校验账号状态和申请归属。
    private Application findOwnedApplication(
            Long applicationId,
            Long applicantId) {

        Applicant applicant = applicants.findById(applicantId)
                .orElseThrow(() -> new NotFoundException(
                        "No applicant with id " + applicantId + "."
                ));

        UserAccount account = applicant.getAccount();

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

        UserAccount account = applicant.getAccount();

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