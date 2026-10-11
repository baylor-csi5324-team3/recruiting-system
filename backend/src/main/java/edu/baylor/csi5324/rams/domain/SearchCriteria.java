package edu.baylor.csi5324.rams.domain;

/** What an applicant may search by (FR 3.1.3). Every field is optional; blank means "any". */
public record SearchCriteria(String keyword, String location, String employmentType) {

    public static final SearchCriteria NONE = new SearchCriteria(null, null, null);

    public boolean hasKeyword() {
        return keyword != null && !keyword.isBlank();
    }

    public boolean hasLocation() {
        return location != null && !location.isBlank();
    }

    public boolean hasEmploymentType() {
        return employmentType != null && !employmentType.isBlank();
    }
}
