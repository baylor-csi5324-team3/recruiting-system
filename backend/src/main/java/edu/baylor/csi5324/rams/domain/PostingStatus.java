package edu.baylor.csi5324.rams.domain;

/** Lifecycle of a {@link JobPosting}. Only PUBLISHED postings are returned by applicant searches. */
public enum PostingStatus {
    DRAFT, PUBLISHED, CLOSED
}
