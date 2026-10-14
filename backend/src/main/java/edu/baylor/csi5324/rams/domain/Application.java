package edu.baylor.csi5324.rams.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "job_applications",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"applicant_id", "job_posting_id"}
        )
)
public class Application {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "applicant_id", nullable = false)
    private Applicant applicant;

    @ManyToOne(optional = false)
    @JoinColumn(name = "job_posting_id", nullable = false)
    private JobPosting jobPosting;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApplicationStage stage = ApplicationStage.SUBMITTED;

    @Column(nullable = false)
    private LocalDateTime submitDate;

    private LocalDateTime respondedAt;

    /** Snapshot of the resume at submission; later resume edits cannot change it. */
    private String resumeFilename;

    @Lob
    private byte[] submittedResume;

    @Lob
    private String coverLetter;

    @Version
    private Long version;

    protected Application() {
    }

    public Application(
            Applicant applicant,
            JobPosting jobPosting,
            LocalDateTime submitDate) {

        this.applicant = applicant;
        this.jobPosting = jobPosting;
        this.submitDate = submitDate;
        this.stage = ApplicationStage.SUBMITTED;
    }

    /** Steps 7-8: snapshot submitted materials while preserving the existing stage workflow. */
    public static Application submittedWithMaterials(
            Applicant applicant, JobPosting job, LocalDateTime time,
            Resume resume, String coverLetter) {
        Application application = new Application(applicant, job, time);
        application.resumeFilename = resume.getFilename();
        application.submittedResume = resume.getContent();
        application.coverLetter = coverLetter;
        return application;
    }

    public String getResumeFilename() { return resumeFilename; }
    public byte[] getSubmittedResume() {
        return submittedResume == null ? null : submittedResume.clone();
    }
    public String getCoverLetter() { return coverLetter; }

    public boolean belongsTo(Applicant applicant) {
        return applicant != null
                && applicant.getId() != null
                && applicant.getId().equals(this.applicant.getId());
    }

    // 招聘者发出 Offer；身份及公司权限由 Service 检查。
    public void makeOffer() {
        if (stage != ApplicationStage.UNDER_REVIEW
                && stage != ApplicationStage.INTERVIEW) {
            throw new IllegalStateException(
                    "An offer can only be made after review or interview."
            );
        }

        this.stage = ApplicationStage.OFFER;
    }

    public void recordStage(
            ApplicationStage nextStage,
            LocalDateTime respondedAt) {

        if (stage != ApplicationStage.OFFER) {
            throw new IllegalStateException(
                    "Only an application in OFFER stage can be accepted or declined."
            );
        }

        if (nextStage != ApplicationStage.ACCEPTED
                && nextStage != ApplicationStage.DECLINED) {
            throw new IllegalArgumentException(
                    "The response stage must be ACCEPTED or DECLINED."
            );
        }

        if (respondedAt == null) {
            throw new IllegalArgumentException("Response time is required.");
        }

        this.stage = nextStage;
        this.respondedAt = respondedAt;
    }

    public void startReview() {
        if (stage != ApplicationStage.SUBMITTED) {
            throw new IllegalStateException(
                    "Only a submitted application can enter review."
            );
        }

        this.stage = ApplicationStage.UNDER_REVIEW;
    }

    public ApplicationSummary getDetails() {
        return ApplicationSummary.from(this);
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

    public ApplicationStage getStage() {
        return stage;
    }

    public LocalDateTime getSubmitDate() {
        return submitDate;
    }

    public LocalDateTime getRespondedAt() {
        return respondedAt;
    }

    public Long getVersion() {
        return version;
    }
}