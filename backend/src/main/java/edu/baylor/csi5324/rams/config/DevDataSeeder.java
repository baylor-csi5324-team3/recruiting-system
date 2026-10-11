package edu.baylor.csi5324.rams.config;

import edu.baylor.csi5324.rams.domain.AccountStatus;
import edu.baylor.csi5324.rams.domain.Administrator;
import edu.baylor.csi5324.rams.domain.Company;
import edu.baylor.csi5324.rams.domain.JobPosting;
import edu.baylor.csi5324.rams.domain.PostingDetails;
import edu.baylor.csi5324.rams.domain.Recruiter;
import edu.baylor.csi5324.rams.domain.Role;
import edu.baylor.csi5324.rams.domain.UserAccount;
import edu.baylor.csi5324.rams.repository.AdministratorRepository;
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
import edu.baylor.csi5324.rams.domain.Applicant;
import edu.baylor.csi5324.rams.domain.Application;
import edu.baylor.csi5324.rams.repository.ApplicantRepository;
import edu.baylor.csi5324.rams.repository.ApplicationRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Development data for the in-memory H2 database so that the three use cases can be exercised
 * right after start-up. Not active under the "test" profile. The sample job data file
 * (jobs.csv) and its loader will replace the hard-coded postings.
 */
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
            Clock clock) {

        return args -> {
            if (accounts.count() > 0) {
                return;
            }
            LocalDate today = LocalDate.now(clock);
            LocalDateTime now = LocalDateTime.now(clock);

            UserAccount adminAccount = accounts.save(new UserAccount("admin@example.com", Role.ADMINISTRATOR, AccountStatus.ACTIVE));
            Administrator admin = administrators.save(new Administrator("Ada Admin", adminAccount));

            Company company = companies.save(new Company("Baylor Tech"));
            accounts.save(new UserAccount("recruiter@example.com", Role.RECRUITER, AccountStatus.ACTIVE));
            Recruiter recruiter = recruiters.save(new Recruiter("Rhea Recruiter", company));

            UserAccount applicantAccount = accounts.save(
                    new UserAccount(
                            "applicant@example.com",
                            Role.APPLICANT,
                            AccountStatus.ACTIVE
                    )
            );

            Applicant applicant = applicants.save(
                    new Applicant("Peter Chen", applicantAccount)
            );
            accounts.save(new UserAccount("dormant@example.com", Role.APPLICANT, AccountStatus.DEACTIVATED));

            JobPosting backend = new JobPosting(new PostingDetails("Backend Developer",
                    "Build Spring Boot services for the recruiting platform.", "Waco, TX", "FULL_TIME",
                    today.plusDays(30)), recruiter, now.minusDays(3));
            backend.markPublished(recruiter, now.minusDays(2));
            JobPosting intern = new JobPosting(new PostingDetails("Software Engineering Intern",
                    "Summer internship on the web team.", "Austin, TX", "INTERNSHIP",
                    today.plusDays(45)), recruiter, now.minusDays(1));
            intern.markPublished(recruiter, now.minusHours(5));
            JobPosting frontend = new JobPosting(new PostingDetails("Frontend Developer",
                    "React dashboards for recruiters and applicants.", "Waco, TX", "PART_TIME",
                    today.plusDays(10)), recruiter, now.minusDays(5));
            frontend.markPublished(recruiter, now.minusDays(4));
            JobPosting expired = new JobPosting(new PostingDetails("Data Analyst",
                    "Closed last week; must not appear in searches.", "Dallas, TX", "FULL_TIME",
                    today.minusDays(7)), recruiter, now.minusDays(40));
            expired.markPublished(recruiter, now.minusDays(39));
            JobPosting draft = new JobPosting(new PostingDetails("QA Engineer",
                    "Still a draft; must not appear in searches.", "Waco, TX", "FULL_TIME",
                    today.plusDays(20)), recruiter, now);
            postings.saveAll(java.util.List.of(backend, intern, frontend, expired, draft));
            Application firstApplication = new Application(
                    applicant,
                    backend,
                    now
            );
            firstApplication.startReview();
            firstApplication.makeOffer();
            applications.save(firstApplication);

            Application secondApplication = new Application(
                    applicant,
                    intern,
                    now
            );
            secondApplication.startReview();
            secondApplication.makeOffer();
            applications.save(secondApplication);

            log.info(
                    "Offer test data: applicantId={}, applicationIds=[{}, {}]",
                    applicant.getId(),
                    firstApplication.getId(),
                    secondApplication.getId()
            );
            log.info("Seeded development data: administrator id {}, recruiter id {}, {} postings",
                    admin.getId(), recruiter.getId(), postings.count());
        };
    }
}
