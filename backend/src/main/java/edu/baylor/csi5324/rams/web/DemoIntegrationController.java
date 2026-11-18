package edu.baylor.csi5324.rams.web;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.core.JacksonException;
import edu.baylor.csi5324.rams.domain.*;
import edu.baylor.csi5324.rams.repository.*;
import edu.baylor.csi5324.rams.service.NotFoundException;
import edu.baylor.csi5324.rams.service.ValidationException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/** Read models and small extras for the React front end (applications, saved jobs).
 * Jobs: JobListController. Users and accounts: AccountController.
 * The applicant profile is served by ApplicantProfileController (Update Applicant Profile).
 * Existing application, posting and account business rules remain in their services. */
@RestController
@RequestMapping("/api/demo")
@Transactional
public class DemoIntegrationController {
    private final ApplicantRepository applicants;
    private final RecruiterRepository recruiters;
    private final AdministratorRepository admins;
    private final JobPostingRepository jobs;
    private final ApplicationRepository applications;
    private final UserAccountRepository accounts;
    private final ResumeRepository resumes;
    private final ObjectMapper mapper;
    private final Path stateFile = Paths.get("data", "demo-workspace.json");

    public DemoIntegrationController(ApplicantRepository applicants, RecruiterRepository recruiters,
            AdministratorRepository admins, JobPostingRepository jobs,
            ApplicationRepository applications, UserAccountRepository accounts,
            ResumeRepository resumes, ObjectMapper mapper) {
        this.applicants = applicants;
        this.recruiters = recruiters;
        this.admins = admins;
        this.jobs = jobs;
        this.applications = applications;
        this.accounts = accounts;
        this.resumes = resumes;
        this.mapper = mapper;
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

    @GetMapping("/saved/{id}")
    public List<Long> saved(@PathVariable Long id) {
        requireApplicant(id);
        Object value = section("saved-"+id).get("ids");
        if (!(value instanceof List<?> v)) return List.of();
        return v.stream().filter(Number.class::isInstance).map(x -> ((Number)x).longValue()).toList();
    }
    @PutMapping("/saved/{id}")
    public List<Long> updateSaved(@PathVariable Long id, @RequestBody List<Long> ids) {
        requireApplicant(id);
        for (Long jobId: ids) if (!jobs.existsById(jobId)) throw new ValidationException("Unknown job id: " + jobId);
        putSection("saved-"+id, Map.of("ids", ids));
        return saved(id);
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
    private void requireAdmin(Long id) {
        Administrator a = admins.findById(id).orElseThrow(() -> new NotFoundException("Administrator not found."));
        if (a.getRole()!=Role.ADMINISTRATOR || a.getStatus()!=AccountStatus.ACTIVE)
            throw new ValidationException("An active administrator account is required.");
    }

    private synchronized Map<String,Object> section(String key) {
        return new HashMap<>(allState().getOrDefault(key, Map.of()));
    }
    private synchronized Map<String,Map<String,Object>> allState() {
        if (!Files.exists(stateFile)) return new HashMap<>();
        try { return mapper.readValue(stateFile.toFile(), new TypeReference<>() {}); }
        catch (JacksonException e) {
            throw new IllegalStateException(
                    "Cannot read demo workspace file", e
            );
        }}
    private synchronized void putSection(String key, Map<String,?> values) {
        Map<String,Map<String,Object>> state = allState();
        state.put(key, new HashMap<>(values));
        try {
            Files.createDirectories(stateFile.getParent());
            Path tmp = stateFile.resolveSibling(stateFile.getFileName()+".tmp");
            mapper.writerWithDefaultPrettyPrinter().writeValue(tmp.toFile(), state);
            try { Files.move(tmp, stateFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
            catch (AtomicMoveNotSupportedException e) { Files.move(tmp, stateFile, StandardCopyOption.REPLACE_EXISTING); }
        } catch (IOException e) { throw new IllegalStateException("Cannot save demo workspace file", e); }
    }
}
