package edu.baylor.csi5324.rams.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * A job opening. Created as a DRAFT by a recruiter, becomes visible to applicants when PUBLISHED,
 * and is no longer returned by searches once its closing date has passed.
 */
@Entity
public class JobPosting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, length = 4000)
    private String description;

    @Column(nullable = false)
    private String location;

    @Column(nullable = false)
    private String employmentType;

    /** Publication time; null while the posting is a draft. */
    private LocalDateTime postedDate;

    @Column(nullable = false)
    private LocalDate closingDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PostingStatus status = PostingStatus.DRAFT;

    @ManyToOne(optional = false)
    private Company company;

    /** Recruiter who created the posting; also the one recorded at publication. */
    @ManyToOne(optional = false)
    private Recruiter recruiter;

    private LocalDateTime createdAt;

    protected JobPosting() {
    }

    /** «create» JobPosting(details, recruiter): a new draft owned by the recruiter's company. */
    public JobPosting(PostingDetails details, Recruiter recruiter, LocalDateTime createdAt) {
        this.title = details.title().trim();
        this.description = details.description().trim();
        this.location = details.location().trim();
        this.employmentType = details.employmentType().trim().toUpperCase();
        this.closingDate = details.closingDate();
        this.recruiter = recruiter;
        this.company = recruiter.getCompany();
        this.createdAt = createdAt;
        this.status = PostingStatus.DRAFT;
    }

    /** SD message 2.2.1 of Search Available Jobs: open on the given day and matching every criterion. */
    public boolean isOpenAndMatches(SearchCriteria criteria, LocalDate today) {
        if (status != PostingStatus.PUBLISHED || closingDate.isBefore(today)) {
            return false;
        }
        if (criteria.hasKeyword() && !title.toLowerCase().contains(criteria.keyword().trim().toLowerCase())) {
            return false;
        }
        if (criteria.hasLocation() && !location.equalsIgnoreCase(criteria.location().trim())) {
            return false;
        }
        return !criteria.hasEmploymentType()
                || employmentType.equalsIgnoreCase(criteria.employmentType().trim());
    }

    /** SD message 2.3: what an applicant sees in a search result (uses Company.getName()). */
    @JsonIgnore
    public PostingSummary getSummary() {
        return new PostingSummary(id, title, company.getName(), location, employmentType, closingDate);
    }

    /** SD message 3.1.1 of Create and Publish a Job Posting. */
    public void markPublished(Recruiter recruiter, LocalDateTime postedDate) {
        this.recruiter = recruiter;
        this.postedDate = postedDate;
        this.status = PostingStatus.PUBLISHED;
    }

    public boolean belongsTo(Company other) {
        return company.getId() != null && company.getId().equals(other.getId());
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getLocation() {
        return location;
    }

    public String getEmploymentType() {
        return employmentType;
    }

    public LocalDateTime getPostedDate() {
        return postedDate;
    }

    public LocalDate getClosingDate() {
        return closingDate;
    }

    public PostingStatus getStatus() {
        return status;
    }

    public Company getCompany() {
        return company;
    }

    public Recruiter getRecruiter() {
        return recruiter;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
