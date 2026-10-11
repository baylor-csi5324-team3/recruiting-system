package edu.baylor.csi5324.rams.domain;

/** Optional filters that accompany the keyword in a job search (FR 3.1.3: location and employment type). */
public record SearchFilters(String location, String employmentType) {
}
