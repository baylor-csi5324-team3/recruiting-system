package edu.baylor.csi5324.rams.domain;

import java.time.LocalDate;

/** One row of a search result: title, company, location, employment type and closing date (UC step 7). */
public record PostingSummary(Long id, String title, String companyName, String location,
                             String employmentType, LocalDate closingDate) {
}
