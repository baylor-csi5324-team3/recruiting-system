package edu.baylor.csi5324.rams.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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

    @ElementCollection
    @CollectionTable(
            name = "application_stage_history",
            joinColumns = @JoinColumn(name = "application_id")
    )
    @OrderColumn(name = "history_position")
    private List<StageRecord> stageHistory = new ArrayList<>();

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
        Long submittedByUserId = applicant != null ? applicant.getId() : null;

        appendStageRecord(
                ApplicationStage.SUBMITTED,
                submitDate,
                submittedByUserId
        );
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

    // The recruiter extends an offer; identity and company permission are checked by the service.
    public void makeOffer() {
        makeOffer(LocalDateTime.now(), null);
    }

    public void makeOffer(
            LocalDateTime changedAt,
            Long changedByUserId) {

        if (stage != ApplicationStage.UNDER_REVIEW
                && stage != ApplicationStage.INTERVIEW) {
            throw new IllegalStateException(
                    "An offer can only be made after review or interview."
            );
        }

        if (changedAt == null) {
            throw new IllegalArgumentException("Stage change time is required.");
        }

        appendStageRecord(
                ApplicationStage.OFFER,
                changedAt,
                changedByUserId
        );

        this.stage = ApplicationStage.OFFER;
    }

    /**
     * Updates the application stage.
     * Recruitment activities are handled outside the system.
     */
    public void updateStage(
            ApplicationStage nextStage,
            LocalDateTime changedAt) {
        updateStage(nextStage, changedAt, null);
    }

    public void updateStage(
            ApplicationStage nextStage,
            LocalDateTime changedAt,
            Long changedByUserId) {

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

            // Cannot withdraw after recieving an offer, only accept or decline
            case OFFER ->
                    nextStage == ApplicationStage.ACCEPTED
                            || nextStage == ApplicationStage.DECLINED;

            case ACCEPTED, DECLINED, REJECTED, WITHDRAWN -> false;
        };

        if (!allowed) {
            throw new IllegalStateException(
                    "Invalid application stage transition: "
                            + stage + " -> " + nextStage);
        }

        appendStageRecord(
                nextStage,
                changedAt,
                changedByUserId
        );

        this.stage = nextStage;
        this.stageUpdatedAt = changedAt;
    }

    public void startReview() {
        startReview(LocalDateTime.now(), null);
    }

    public void startReview(
            LocalDateTime changedAt,
            Long changedByUserId) {

        if (stage != ApplicationStage.SUBMITTED) {
            throw new IllegalStateException(
                    "Only a submitted application can enter review."
            );
        }

        if (changedAt == null) {
            throw new IllegalArgumentException("Stage change time is required.");
        }

        appendStageRecord(
                ApplicationStage.UNDER_REVIEW,
                changedAt,
                changedByUserId
        );

        this.stage = ApplicationStage.UNDER_REVIEW;
    }

    public boolean checkWithdrawable() {
        return stage == ApplicationStage.SUBMITTED
                || stage == ApplicationStage.UNDER_REVIEW
                || stage == ApplicationStage.INTERVIEW;
    }

    public void withdraw(
            LocalDateTime changedAt,
            Long changedByUserId) {

        if (!checkWithdrawable()) {
            throw new IllegalStateException(
                    "Only applications in SUBMITTED, UNDER_REVIEW, "
                            + "or INTERVIEW can be withdrawn."
            );
        }

        updateStage(
                ApplicationStage.WITHDRAWN,
                changedAt,
                changedByUserId
        );
    }

    public List<StageRecord> getStageHistory() {
        return List.copyOf(stageHistory);
    }

    private void appendStageRecord(
            ApplicationStage recordedStage,
            LocalDateTime changedAt,
            Long changedByUserId) {

        stageHistory.add(
                new StageRecord(
                        recordedStage,
                        changedAt,
                        changedByUserId
                )
        );
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