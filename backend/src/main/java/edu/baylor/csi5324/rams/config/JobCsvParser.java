package edu.baylor.csi5324.rams.config;

import edu.baylor.csi5324.rams.domain.PostingDetails;
import edu.baylor.csi5324.rams.domain.PostingStatus;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Parses and validates sample CSV without accessing the database. */
@Component
public class JobCsvParser {
    /** Validated CSV values; rowNumber excludes the header, even for quoted multiline fields. */
    public record JobCsvRow(int rowNumber, PostingDetails details, PostingStatus status, String recruiterEmail) {
    }

    private static final List<String> HEADERS = List.of("title", "description", "location", "employmentType",
            "closingDate", "status", "recruiterEmail");

    public List<JobCsvRow> parse(Reader reader) {
        try (var csv = CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true)
                .setIgnoreEmptyLines(false).get().parse(reader)) {
            if (!csv.getHeaderNames().equals(HEADERS)) {
                throw new JobCsvException("jobs.csv: expected headers in order: " + String.join(",", HEADERS));
            }
            List<JobCsvRow> rows = new ArrayList<>();
            for (CSVRecord record : csv.getRecords()) {
                int row = Math.toIntExact(record.getRecordNumber());
                if (record.size() != HEADERS.size()) throw invalid(row, "expected exactly seven fields");
                String title = field(record, "title", 255, row);
                String description = field(record, "description", 4000, row);
                String location = field(record, "location", 255, row);
                String type = field(record, "employmentType", 255, row);
                String date = field(record, "closingDate", 10, row);
                LocalDate closing;
                try {
                    if (!date.matches("\\d{4}-\\d{2}-\\d{2}")) throw invalid(row, "closingDate must be YYYY-MM-DD");
                    closing = LocalDate.parse(date);
                } catch (DateTimeParseException e) {
                    throw invalid(row, "closingDate must be a valid YYYY-MM-DD date");
                }
                String statusText = field(record, "status", 20, row);
                PostingStatus status;
                try {
                    status = PostingStatus.valueOf(statusText);
                } catch (IllegalArgumentException e) {
                    throw invalid(row, "status must be DRAFT or PUBLISHED");
                }
                if (status != PostingStatus.DRAFT && status != PostingStatus.PUBLISHED) {
                    throw invalid(row, "status must be DRAFT or PUBLISHED");
                }
                String email = field(record, "recruiterEmail", 255, row).toLowerCase(Locale.ROOT);
                rows.add(new JobCsvRow(row, new PostingDetails(title, description, location, type, closing), status, email));
            }
            if (rows.isEmpty()) throw new JobCsvException("jobs.csv: provide at least one data row.");
            return List.copyOf(rows);
        } catch (IOException | UncheckedIOException | IllegalArgumentException e) {
            throw new JobCsvException("jobs.csv: malformed CSV or unreadable input; check quoting and field structure.");
        }
    }

    private String field(CSVRecord record, String name, int maxLength, int row) {
        String value = record.get(name).trim();
        if (value.isBlank()) throw invalid(row, "missing " + name);
        if (value.length() > maxLength) throw invalid(row, name + " exceeds " + maxLength + " characters");
        return value;
    }

    private JobCsvException invalid(int row, String problem) {
        return new JobCsvException("jobs.csv data row " + row + ": " + problem + ".");
    }
}
