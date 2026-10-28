package edu.baylor.csi5324.rams.config;

import edu.baylor.csi5324.rams.domain.AccountStatus;
import edu.baylor.csi5324.rams.domain.JobPosting;
import edu.baylor.csi5324.rams.domain.Recruiter;
import edu.baylor.csi5324.rams.repository.JobPostingRepository;
import edu.baylor.csi5324.rams.repository.UserAccountRepository;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.ArrayList;

/** Atomic, empty-table initialization for a single development application instance. */
@Service
@Transactional
public class JobCsvLoader {
    private final JobCsvParser parser;
    private final UserAccountRepository accounts;
    private final JobPostingRepository postings;
    private final JobFixtureFactory fixtures;

    public JobCsvLoader(JobCsvParser parser, UserAccountRepository accounts, JobPostingRepository postings, Clock clock) {
        this.parser = parser;
        this.accounts = accounts;
        this.postings = postings;
        this.fixtures = new JobFixtureFactory(clock);
    }

    public int loadIfEmpty() {
        return loadIfEmpty(new ClassPathResource("jobs.csv"));
    }

    public int loadIfEmpty(Resource resource) {
        if (postings.count() != 0) return 0;
        try (var reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)) {
            var jobs = new ArrayList<JobPosting>();
            for (JobCsvParser.JobCsvRow row : parser.parse(reader)) {
                String prefix = "jobs.csv data row " + row.rowNumber() + ": ";
                var account = accounts.findByEmailIgnoreCase(row.recruiterEmail())
                        .orElseThrow(() -> new JobCsvException(prefix + "recruiter account does not exist."));
                if (!(account instanceof Recruiter recruiter) || account.getStatus() != AccountStatus.ACTIVE) {
                    throw new JobCsvException(prefix + "recruiter account must be ACTIVE with role RECRUITER.");
                }
                if (recruiter.getCompany() == null || recruiter.getCompany().getId() == null) {
                    throw new JobCsvException(prefix + "recruiter has no company.");
                }
                jobs.add(fixtures.create(row.details(), row.status(), recruiter, row.rowNumber()));
            }
            postings.saveAllAndFlush(jobs);
            return jobs.size();
        } catch (IOException e) {
            throw new JobCsvException("Cannot read the sample job CSV resource.");
        }
    }
}
