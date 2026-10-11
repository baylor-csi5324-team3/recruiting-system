package edu.baylor.csi5324.rams.domain;

import java.time.LocalDate;

/** The details a recruiter provides for a new posting (UC Create and Publish a Job Posting, step 3). */
public record PostingDetails(String title, String description, String location,
                             String employmentType, LocalDate closingDate) {
}
