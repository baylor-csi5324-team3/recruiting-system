package edu.baylor.csi5324.rams.config;

import edu.baylor.csi5324.rams.domain.AccountStatus;
import edu.baylor.csi5324.rams.domain.Administrator;
import edu.baylor.csi5324.rams.domain.Applicant;
import edu.baylor.csi5324.rams.domain.Application;
import edu.baylor.csi5324.rams.domain.Company;
import edu.baylor.csi5324.rams.domain.JobPosting;
import edu.baylor.csi5324.rams.domain.Recruiter;
import edu.baylor.csi5324.rams.domain.Role;
import edu.baylor.csi5324.rams.domain.UserAccount;
import edu.baylor.csi5324.rams.repository.AdministratorRepository;
import edu.baylor.csi5324.rams.repository.ApplicantRepository;
import edu.baylor.csi5324.rams.repository.ApplicationRepository;
import edu.baylor.csi5324.rams.repository.CompanyRepository;
import edu.baylor.csi5324.rams.repository.JobPostingRepository;
import edu.baylor.csi5324.rams.repository.RecruiterRepository;
import edu.baylor.csi5324.rams.repository.UserAccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import edu.baylor.csi5324.rams.domain.ApplicationStage;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/** Seeds actors, CSV jobs, and sample offers in dependency order within one transaction. */
@Configuration
@Profile("!test")
public class DevDataSeeder {
    private static final Logger log = LoggerFactory.getLogger(DevDataSeeder.class);

    @Bean
    CommandLineRunner seedDevelopmentData(
            CompanyRepository companies,
            RecruiterRepository recruiters,
            AdministratorRepository administrators,
            UserAccountRepository accounts,
            JobPostingRepository postings,
            ApplicantRepository applicants,
            ApplicationRepository applications,
            Clock clock,
            PasswordEncoder encoder,
            JobCsvLoader loader,
            PlatformTransactionManager transactions) {
        return args -> new TransactionTemplate(transactions).executeWithoutResult(transaction -> {
            UserAccount adminAccount = account(accounts, encoder, "admin@example.com", Role.ADMINISTRATOR, AccountStatus.ACTIVE);
            Administrator admin = administrators.findAll().stream()
                    .filter(candidate -> candidate.getAccount() != null && adminAccount.getId().equals(candidate.getAccount().getId()))
                    .findFirst().orElseGet(() -> administrators.save(new Administrator("Ada Admin", adminAccount)));
            UserAccount recruiterAccount = account(accounts, encoder, "recruiter@example.com", Role.RECRUITER, AccountStatus.ACTIVE);
            Recruiter recruiter = recruiters.findByAccountId(recruiterAccount.getId()).orElseGet(() -> {
                Company company = companies.findByNameIgnoreCase("Baylor Tech")
                        .orElseGet(() -> companies.save(new Company("Baylor Tech")));
                return recruiters.save(new Recruiter("Rhea Recruiter", recruiterAccount, company));
            });
            UserAccount applicantAccount = account(accounts, encoder, "applicant@example.com", Role.APPLICANT, AccountStatus.ACTIVE);
            Applicant applicant = applicants.findAll().stream()
                    .filter(candidate -> applicantAccount.getId().equals(candidate.getAccount().getId()))
                    .findFirst().orElseGet(() -> applicants.save(new Applicant("Peter Chen", "254-555-0100", applicantAccount)));
            account(accounts, encoder, "dormant@example.com", Role.APPLICANT, AccountStatus.DEACTIVATED);

            int imported = loader.loadIfEmpty();
            List<JobPosting> jobs = postings.findAll();
            LocalDateTime now = LocalDateTime.now(clock);
            seedOffer(applications, applicant, recruiter, jobs, "Backend Developer", now);
            seedOffer(applications, applicant, recruiter, jobs, "Software Engineering Intern", now);

            log.info("Development data ready: administrator id {}, recruiter id {}, {} CSV postings imported",
                    admin.getId(), recruiter.getId(), imported);
        });
    }

    private UserAccount account(UserAccountRepository accounts, PasswordEncoder encoder, String email,
                                Role role, AccountStatus status) {
        // Newly seeded accounts have random, discarded secrets, not shared default credentials.
        // Existing accounts and their credentials/roles/statuses are never overwritten.
        return accounts.findByEmailIgnoreCase(email).orElseGet(() -> accounts.save(new UserAccount(
                email, encoder.encode(UUID.randomUUID().toString()), role, status)));
    }

    private void seedOffer(ApplicationRepository applications, Applicant applicant, Recruiter recruiter,
                           List<JobPosting> jobs, String title, LocalDateTime now) {
        List<JobPosting> matches = jobs.stream()
                .filter(job -> title.equals(job.getTitle())
                        && recruiter.getId().equals(job.getRecruiter().getId())
                        && recruiter.getCompany().getId().equals(job.getCompany().getId()))
                .toList();
        // A populated job table may contain custom data: never invent or guess a replacement job.
        if (matches.size() != 1) {
            log.warn("Skipping development offer for applicantId={} and job '{}': expected one posting for recruiterId={}, found {}",
                    applicant.getId(), title, recruiter.getId(), matches.size());
            return;
        }
        JobPosting job = matches.get(0);
        // Preserve an existing application's stage and any submitted materials on repeated startup.
        if (applications.existsByApplicantIdAndJobPostingId(applicant.getId(), job.getId())) {
            return;
        }
        Application application = new Application(applicant, job, now);
        application.startReview(now, recruiter.getAccount().getId());
        application.makeOffer(now, recruiter.getAccount().getId());
        applications.save(application);
        log.info("Offer test data: applicantId={}, applicationId={}, jobPostingId={}",
                applicant.getId(), application.getId(), job.getId());
    }
}
