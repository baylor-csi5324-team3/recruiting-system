
package edu.baylor.csi5324.rams.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "saved_jobs",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"applicant_id", "job_posting_id"}
        )
)
public class SavedJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "applicant_id", nullable = false)
    private Applicant applicant;

    @ManyToOne(optional = false)
    @JoinColumn(name = "job_posting_id", nullable = false)
    private JobPosting jobPosting;

    @Column(nullable = false)
    private LocalDateTime savedAt;

    protected SavedJob() {
    }

    public SavedJob(
            Applicant applicant,
            JobPosting jobPosting,
            LocalDateTime savedAt) {

        this.applicant = applicant;
        this.jobPosting = jobPosting;
        this.savedAt = savedAt;
    }

    public Long getId() {
        return id;
    }

    public Applicant getApplicant() {
        return applicant;
    }

    public JobPosting getJobPosting() {
        return jobPosting;
    }

    public LocalDateTime getSavedAt() {
        return savedAt;
    }
}
