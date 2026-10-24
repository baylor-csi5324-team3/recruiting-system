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

    private LocalDateTime stageUpdatedAt;

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



    /**
     * Updates the application stage.
     * Recruitment activities are handled outside the system.
     */
    public void updateStage(
            ApplicationStage nextStage,
            LocalDateTime changedAt) {

        if (nextStage == null) {
            throw new IllegalArgumentException(
                    "Application stage is required.");
        }

        if (changedAt == null) {
            throw new IllegalArgumentException(
                    "Stage update time is required.");
        }

        boolean allowed = switch (stage) {
            case SUBMITTED ->
                    nextStage == ApplicationStage.UNDER_REVIEW
                            || nextStage == ApplicationStage.REJECTED
                            || nextStage == ApplicationStage.WITHDRAWN;

            case UNDER_REVIEW ->
                    nextStage == ApplicationStage.INTERVIEW
                            || nextStage == ApplicationStage.OFFER
                            || nextStage == ApplicationStage.REJECTED
                            || nextStage == ApplicationStage.WITHDRAWN;

            case INTERVIEW ->
                    nextStage == ApplicationStage.OFFER
                            || nextStage == ApplicationStage.REJECTED
                            || nextStage == ApplicationStage.WITHDRAWN;

            case OFFER ->
                    nextStage == ApplicationStage.ACCEPTED
                            || nextStage == ApplicationStage.DECLINED
                            || nextStage == ApplicationStage.WITHDRAWN;

            case ACCEPTED, DECLINED, REJECTED, WITHDRAWN -> false;
        };

        if (!allowed) {
            throw new IllegalStateException(
                    "Invalid application stage transition: "
                            + stage + " -> " + nextStage);
        }

        this.stage = nextStage;
        this.stageUpdatedAt = changedAt;
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

    public LocalDateTime getStageUpdatedAt() {
        return stageUpdatedAt;
    }

    public Long getVersion() {
        return version;
    }
}