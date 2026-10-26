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

/** Read models and small extras for the React front end (actors, jobs, applications, accounts, profile, saved jobs).
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

    public record Actor(Long id, String name, String email, String role, String company) {}
    public record JobView(Long id, String title, String description, String companyName, String location,
            String employmentType, LocalDate closingDate, LocalDateTime postedDate, String status) {}
    public record ApplicationView(Long id, Long jobId, String name, String email, String stage,
            LocalDateTime submittedAt, String resumeFilename, List<StageRecord> history) {}
    public record AccountView(Long id, String name, String email, String role, boolean active) {}
    public record ProfileView(String name, String email, String phone, String location,
            String headline, String bio, String resume) {}

    @GetMapping("/actors")
    public List<Actor> actors() {
        List<Actor> result = new ArrayList<>();
        for (Applicant a: applicants.findAll()) {
            if (a.getAccount().getRole()!=Role.APPLICANT || a.getAccount().getStatus()!=AccountStatus.ACTIVE) continue;
            result.add(new Actor(a.getId(), a.getName(), a.getAccount().getEmail(), "Applicant", null));
        }
        for (Recruiter a: recruiters.findAll()) {
            if (a.getAccount().getRole()!=Role.RECRUITER || a.getAccount().getStatus()!=AccountStatus.ACTIVE) continue;
            result.add(new Actor(a.getId(), a.getName(), a.getAccount().getEmail(), "Recruiter", a.getCompany().getName()));
        }
        for (Administrator a: admins.findAll()) {
            if (a.getAccount() != null && a.getAccount().getRole()==Role.ADMINISTRATOR && a.getAccount().getStatus()==AccountStatus.ACTIVE) result.add(new Actor(a.getId(), a.getName(), a.getAccount().getEmail(), "Administrator", null));
        }
        return result;
    }

    private JobView job(JobPosting p) {
        return new JobView(p.getId(), p.getTitle(), p.getDescription(), p.getCompany().getName(),
                p.getLocation(), p.getEmploymentType(), p.getClosingDate(), p.getPostedDate(), p.getStatus().name());
    }

    @GetMapping("/jobs")
    public List<JobView> listJobs(@RequestParam(required = false) Long recruiterId) {
        Recruiter recruiter = recruiterId == null ? null : requireRecruiter(recruiterId);
        return jobs.findAll().stream()
                .filter(p -> p.getStatus() != PostingStatus.DRAFT || (recruiter != null && p.belongsTo(recruiter.getCompany())))
                .map(this::job).toList();
    }

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
                        a.getApplicant().getAccount().getEmail(), a.getStage().name(), a.getSubmitDate(),
                        a.getResumeFilename(), a.getStageHistory())).toList();
    }

    @GetMapping("/accounts")
    public List<AccountView> listAccounts(@RequestParam Long administratorId) {
        requireAdmin(administratorId);
        Map<Long,String> names = new HashMap<>();
        for (Actor a : actors()) {
            String email = a.email();
            accounts.findByEmailIgnoreCase(email).ifPresent(ac -> names.put(ac.getId(), a.name()));
        }
        return accounts.findAll().stream().map(a -> new AccountView(a.getId(),
                names.getOrDefault(a.getId(), a.getEmail()), a.getEmail(),
                role(a.getRole()), a.getStatus() == AccountStatus.ACTIVE)).toList();
    }
    private String role(Role r) { return switch (r) {
        case APPLICANT -> "Applicant";
        case RECRUITER -> "Recruiter";
        case ADMINISTRATOR -> "Administrator";
    }; }

    @GetMapping("/profile/{id}")
    public ProfileView profile(@PathVariable Long id) {
        Applicant a = requireApplicant(id);
        Map<String,Object> extra = section("profile-"+id);
        String currentResume = resumes.findByApplicantIdOrderByIdDesc(id).stream()
                .findFirst().map(Resume::getFilename).orElse("");
        return new ProfileView(a.getName(), a.getAccount().getEmail(), a.getPhone(),
                (String)extra.getOrDefault("location", ""), (String)extra.getOrDefault("headline", ""),
                (String)extra.getOrDefault("bio", ""), currentResume);
    }
    @PutMapping("/profile/{id}")
    public ProfileView updateProfile(@PathVariable Long id, @RequestBody ProfileView data) {
        Applicant a = requireApplicant(id);
        if (data.name() == null || data.name().isBlank()) throw new ValidationException("Name is required.");
        a.setName(data.name().trim());
        a.setPhone(data.phone());
        applicants.save(a);
        putSection("profile-"+id, Map.of("location", safe(data.location()),
                "headline", safe(data.headline()), "bio", safe(data.bio())));
        return profile(id);
    }
    private static String safe(String s) { return s == null ? "" : s; }

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
        if (a.getAccount().getRole()!=Role.APPLICANT || a.getAccount().getStatus()!=AccountStatus.ACTIVE)
            throw new ValidationException("An active applicant account is required.");
        return a;
    }
    private Recruiter requireRecruiter(Long id) {
        Recruiter a = recruiters.findById(id).orElseThrow(() -> new NotFoundException("Recruiter not found."));
        if (a.getAccount().getRole()!=Role.RECRUITER || a.getAccount().getStatus()!=AccountStatus.ACTIVE)
            throw new ValidationException("An active recruiter account is required.");
        return a;
    }
    private void requireAdmin(Long id) {
        Administrator a = admins.findById(id).orElseThrow(() -> new NotFoundException("Administrator not found."));
        if (a.getAccount()==null || a.getAccount().getRole()!=Role.ADMINISTRATOR || a.getAccount().getStatus()!=AccountStatus.ACTIVE)
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
