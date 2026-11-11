package edu.baylor.csi5324.rams.domain;

import java.time.LocalDate;

/** The details a recruiter provides for a new posting (UC Create and Publish a Job Posting, step 3).
 * Qualifications are the skills or experience the job asks for. */
public record PostingDetails(String title, String description, String location,
                             String employmentType, LocalDate closingDate, String qualifications) {

    /** Details without qualifications (job fixtures and older callers). */
    public PostingDetails(String title, String description, String location,
                          String employmentType, LocalDate closingDate) {
        this(title, description, location, employmentType, closingDate, null);
    }
}
