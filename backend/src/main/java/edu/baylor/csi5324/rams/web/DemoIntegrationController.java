package edu.baylor.csi5324.rams.web;

import edu.baylor.csi5324.rams.domain.*;
import edu.baylor.csi5324.rams.repository.*;
import edu.baylor.csi5324.rams.service.NotFoundException;
import edu.baylor.csi5324.rams.service.ValidationException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.*;

/** Read model for the React front end: the applications of an applicant or of a recruiter's company.
 * Jobs: JobListController. Users and accounts: AccountController. Saved jobs: SavedJobController.
 * The applicant profile is served by ApplicantProfileController (Update Applicant Profile).
 * Existing application, posting and account business rules remain in their services. */
@RestController
@RequestMapping("/api/demo")
@Transactional
public class DemoIntegrationController {
    private final ApplicantRepository applicants;
    private final RecruiterRepository recruiters;
    private final ApplicationRepository applications;

    public DemoIntegrationController(ApplicantRepository applicants, RecruiterRepository recruiters,
            ApplicationRepository applications) {
        this.applicants = applicants;
        this.recruiters = recruiters;
        this.applications = applications;
    }

    public record ApplicationView(Long id, Long jobId, String name, String email, String stage,
            LocalDateTime submittedAt, String resumeFilename, List<StageRecord> history) {}

    @GetMapping("/applications")
    public List<ApplicationView> listApplications(@RequestParam(required = false) Long applicantId,
                                                  @RequestParam(required = false) Long recruiterId) {
        if ((applicantId == null) == (recruiterId == null)) throw new ValidationException("Choose an applicant or recruiter.");
        Recruiter recruiter = recruiterId == null ? null : requireRecruiter(recruiterId);
        if (applicantId != null) requireApplicant(applicantId);
        return applications.findAll().stream()
                .filter(a -> applicantId != null ? a.getApplicant().getId().equals(applicantId)
                        : a.getJobPosting().belongsTo(recruiter.getCompany()))
                .map(a -> new ApplicationView(a.getId(), a.getJobPosting().getId(), a.getApplicant().getName(),
                        a.getApplicant().getEmail(), a.getStage().name(), a.getSubmitDate(),
                        a.getResumeFilename(), a.getStageHistory())).toList();
    }

    private Applicant requireApplicant(Long id) {
        Applicant a = applicants.findById(id).orElseThrow(() -> new NotFoundException("Applicant not found."));
        if (a.getRole()!=Role.APPLICANT || a.getStatus()!=AccountStatus.ACTIVE)
            throw new ValidationException("An active applicant account is required.");
        return a;
    }
    private Recruiter requireRecruiter(Long id) {
        Recruiter a = recruiters.findById(id).orElseThrow(() -> new NotFoundException("Recruiter not found."));
        if (a.getRole()!=Role.RECRUITER || a.getStatus()!=AccountStatus.ACTIVE)
            throw new ValidationException("An active recruiter account is required.");
        return a;
    }
}
