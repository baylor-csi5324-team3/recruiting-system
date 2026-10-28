package edu.baylor.csi5324.rams.config;

import edu.baylor.csi5324.rams.domain.AccountStatus;
import edu.baylor.csi5324.rams.domain.Administrator;
import edu.baylor.csi5324.rams.domain.Applicant;
import edu.baylor.csi5324.rams.domain.Application;
import edu.baylor.csi5324.rams.domain.Company;
import edu.baylor.csi5324.rams.domain.JobPosting;
import edu.baylor.csi5324.rams.domain.Recruiter;
import edu.baylor.csi5324.rams.domain.UserAccount;
import edu.baylor.csi5324.rams.repository.ApplicationRepository;
import edu.baylor.csi5324.rams.repository.CompanyRepository;
import edu.baylor.csi5324.rams.repository.JobPostingRepository;
import edu.baylor.csi5324.rams.repository.UserAccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

/** Seeds actors, CSV jobs, and sample offers in dependency order within one transaction. */
@Configuration
@Profile("!test")
public class DevDataSeeder {
    private static final Logger log = LoggerFactory.getLogger(DevDataSeeder.class);

    @Bean
    CommandLineRunner seedDevelopmentData(
            CompanyRepository companies,
            UserAccountRepository accounts,
            JobPostingRepository postings,
            ApplicationRepository applications,
            Clock clock,
            PasswordEncoder encoder,
            JobCsvLoader loader,
            PlatformTransactionManager transactions,
            @Value("${rams.seed.demo-password:}") String demoPassword) {
        return args -> new TransactionTemplate(transactions).executeWithoutResult(transaction -> {
            Administrator admin = account(accounts, Administrator.class, "admin@example.com", email ->
                    new Administrator(email, secret(encoder, demoPassword), "Ada Admin", AccountStatus.ACTIVE));
            Recruiter recruiter = account(accounts, Recruiter.class, "recruiter@example.com", email -> {
                Company company = companies.findByNameIgnoreCase("Baylor Tech")
                        .orElseGet(() -> companies.save(new Company("Baylor Tech")));
                return new Recruiter(email, secret(encoder, demoPassword), "Rhea Recruiter", company, AccountStatus.ACTIVE);
            });
            Applicant applicant = account(accounts, Applicant.class, "applicant@example.com", email ->
                    new Applicant(email, secret(encoder, demoPassword), "Peter Chen", "254-555-0100", AccountStatus.ACTIVE));
            account(accounts, Applicant.class, "dormant@example.com", email ->
                    new Applicant(email, secret(encoder, demoPassword), "Dana Dormant", AccountStatus.DEACTIVATED));

            int imported = loader.loadIfEmpty();
            List<JobPosting> jobs = postings.findAll();
            LocalDateTime now = LocalDateTime.now(clock);
            if (applicant != null && recruiter != null) {
                seedOffer(applications, applicant, recruiter, jobs, "Backend Developer", now);
                seedOffer(applications, applicant, recruiter, jobs, "Software Engineering Intern", now);
            }

            log.info("Development data ready: administrator id {}, recruiter id {}, applicant id {}, {} CSV postings imported",
                    admin == null ? null : admin.getId(), recruiter == null ? null : recruiter.getId(),
                    applicant == null ? null : applicant.getId(), imported);
        });
    }

    /**
     * Creates a sample account of the given role unless the email is already used. Existing accounts and their
     * credentials, roles and statuses are never overwritten; one whose role was changed is left out of the sample data.
     */
    private <T extends UserAccount> T account(UserAccountRepository accounts, Class<T> role, String email,
                                              Function<String, T> create) {
        UserAccount existing = accounts.findByEmailIgnoreCase(email).orElse(null);
        if (existing == null) {
            return accounts.save(create.apply(email));
        }
        if (!role.isInstance(existing)) {
            log.warn("Sample account {} now has role {}; it is not used for the sample data", email, existing.getRole());
            return null;
        }
        return role.cast(existing);
    }

    /** The development demo password (rams.seed.demo-password), or a random, discarded secret without it. */
    private String secret(PasswordEncoder encoder, String demoPassword) {
        return encoder.encode(demoPassword.isBlank() ? UUID.randomUUID().toString() : demoPassword);
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
        application.startReview(now, recruiter.getId());
        application.makeOffer(now, recruiter.getId());
        applications.save(application);
        log.info("Offer test data: applicantId={}, applicationId={}, jobPostingId={}",
                applicant.getId(), application.getId(), job.getId());
    }
}
