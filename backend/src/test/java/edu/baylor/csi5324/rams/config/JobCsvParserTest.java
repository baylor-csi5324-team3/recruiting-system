package edu.baylor.csi5324.rams.config;

import org.junit.jupiter.api.Test;
import java.io.StringReader;
import static org.junit.jupiter.api.Assertions.*;

class JobCsvParserTest {
    private final JobCsvParser parser = new JobCsvParser();
    private static final String HEADER = "title,description,location,employmentType,closingDate,status,recruiterEmail\n";
    private static final String ROW = "Developer,Build services,Waco,FULL_TIME,2026-12-31,PUBLISHED,recruiter@example.com\n";

    @Test
    void parsesQuotedCommasEscapedQuotesAndMultilineDescriptions() {
        var row = parser.parse(new StringReader(HEADER + "Developer,\"Build \"\"great\"\" services,\nfor applicants\",\"Waco, TX\",FULL_TIME,2026-12-31,PUBLISHED, RECRUITER@EXAMPLE.COM \n")).getFirst();
        assertEquals("Build \"great\" services,\nfor applicants", row.details().description());
        assertEquals("Waco, TX", row.details().location());
        assertEquals("recruiter@example.com", row.recruiterEmail());
        assertEquals(1, row.rowNumber());
    }

    @Test
    void requiresExactHeaderOrderAndNoDuplicates() {
        for (String header : new String[]{"", HEADER.replace("title,description", "description,title"), HEADER.replace("title", "description")}) {
            assertThrows(JobCsvException.class, () -> parser.parse(new StringReader(header + ROW)));
        }
    }

    @Test
    void validatesMissingFieldsAndColumnCount() {
        for (String row : new String[]{ROW.replace("Developer", " "), ROW.replace("Build services", ""), ROW.replace("recruiter@example.com", ""), ROW.replace("Developer,", ""), ROW.replace("Developer,", "Developer,extra,")}) {
            assertThrows(JobCsvException.class, () -> parser.parse(new StringReader(HEADER + row)));
        }
    }

    @Test
    void validatesDateStatusAndMappedLength() {
        for (String row : new String[]{ROW.replace("2026-12-31", "2026-02-30"), ROW.replace("2026-12-31", "12/31/2026"), ROW.replace("PUBLISHED", "CLOSED"), ROW.replace("PUBLISHED", "UNKNOWN"), ROW.replace("Build services", "x".repeat(4001))}) {
            assertThrows(JobCsvException.class, () -> parser.parse(new StringReader(HEADER + row)));
        }
    }

    @Test
    void malformedQuotesAndEmptyDatasetFail() {
        assertThrows(JobCsvException.class, () -> parser.parse(new StringReader(HEADER + "\"unclosed")));
        assertThrows(JobCsvException.class, () -> parser.parse(new StringReader(HEADER)));
    }

    @Test
    void invalidSecondRecordNamesItsRowAndField() {
        var error = assertThrows(JobCsvException.class, () -> parser.parse(new StringReader(HEADER + ROW + ROW.replace("Developer", ""))));
        assertTrue(error.getMessage().contains("row 2"));
        assertTrue(error.getMessage().contains("title"));
    }
}
