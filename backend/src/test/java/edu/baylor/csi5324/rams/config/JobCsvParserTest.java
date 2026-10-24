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
    void rejectsIncorrectHeaders() {
        String header = HEADER.replace("title,description", "description,title");
        assertThrows(JobCsvException.class, () -> parser.parse(new StringReader(header + ROW)));
    }

    @Test
    void validatesMissingFieldsAndColumnCount() {
        for (String row : new String[]{ROW.replace("Developer", " "), ROW.replace("Developer,", "")}) {
            assertThrows(JobCsvException.class, () -> parser.parse(new StringReader(HEADER + row)));
        }
    }

    @Test
    void rejectsInvalidDateAndUnsupportedStatus() {
        for (String row : new String[]{ROW.replace("2026-12-31", "2026-02-30"), ROW.replace("PUBLISHED", "CLOSED")}) {
            assertThrows(JobCsvException.class, () -> parser.parse(new StringReader(HEADER + row)));
        }
    }

}
