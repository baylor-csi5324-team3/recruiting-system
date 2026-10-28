package edu.baylor.csi5324.rams.domain;

/** The applicant information shown and changed by Update Applicant Profile; the email is read only. */
public record ApplicantProfile(Long applicantId, String name, String email, String phone,
                               String location, String headline, String bio) {
}
