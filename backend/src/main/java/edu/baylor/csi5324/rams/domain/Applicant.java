package edu.baylor.csi5324.rams.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;

/** Job seeker: a UserAccount with the applicant profile used by Update Applicant Profile. */
@Entity
public class Applicant extends UserAccount {

    private String phone;

    private String location;

    private String headline;

    @Column(length = 2000)
    private String bio;

    // No-argument constructor required by JPA.
    protected Applicant() {
    }

    public Applicant(String email, String passwordHash, String name, AccountStatus status) {
        super(email, passwordHash, name, Role.APPLICANT, status);
    }

    public Applicant(String email, String passwordHash, String name, String phone, AccountStatus status) {
        this(email, passwordHash, name, status);
        this.phone = phone;
    }

    /** Manage Accounts and Roles, SD message 3.1.3.2: the same person, now an applicant. */
    public Applicant(UserAccount previous) {
        super(previous, Role.APPLICANT);
    }

    /** SD 6.4 message 1.1.2: the profile the applicant sees and edits. */
    @JsonIgnore
    public ApplicantProfile getDetails() {
        return new ApplicantProfile(getId(), getName(), getEmail(), phone, location, headline, bio);
    }

    /** SD 6.4 message 5.2: applies already validated changes. */
    public void updateDetails(ApplicantProfile updatedInfo) {
        setName(updatedInfo.name());
        this.phone = updatedInfo.phone();
        this.location = updatedInfo.location();
        this.headline = updatedInfo.headline();
        this.bio = updatedInfo.bio();
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }
}
