package edu.baylor.csi5324.rams.service;

import edu.baylor.csi5324.rams.domain.RegisterAccountRequest;
import edu.baylor.csi5324.rams.domain.Role;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Register Account input rules only; these tests do not claim registration or persistence works. */
class RegistrationValidatorTest {

    private static ValidatorFactory factory;
    private static RegistrationValidator validator;

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = new RegistrationValidator(factory.getValidator());
    }

    @AfterAll
    static void closeFactory() {
        factory.close();
    }

    private RegisterAccountRequest applicant(String email, String password) {
        return new RegisterAccountRequest(email, password, Role.APPLICANT, null, null);
    }

    @Test
    @DisplayName("applicant input needs no company or recruiter name; email is normalized, password is untouched")
    void applicantInput() {
        RegisterAccountRequest request = applicant("  Person@Example.COM  ", "  secret phrase  ");
        assertDoesNotThrow(() -> validator.validate(request));
        assertEquals("person@example.com", request.email());
        assertEquals("  secret phrase  ", request.password());
    }

    @Test
    @DisplayName("recruiter input keeps display capitalization and trims company and recruiter names")
    void recruiterInput() {
        RegisterAccountRequest request = new RegisterAccountRequest("hr@example.com", "secret phrase",
                Role.RECRUITER, "  Baylor Tech  ", "  Rhea Recruiter  ");
        assertDoesNotThrow(() -> validator.validate(request));
        assertEquals("Baylor Tech", request.companyName());
        assertEquals("Rhea Recruiter", request.recruiterName());
    }

    @Test
    void missingRequestRejected() {
        assertThrows(ValidationException.class, () -> validator.validate(null));
    }

    @Test
    void missingAndInvalidEmailRejected() {
        for (String email : new String[]{null, "", " ", "not-an-email", "a@@example.com"}) {
            assertThrows(ValidationException.class, () -> validator.validate(applicant(email, "secret phrase")));
        }
    }

    @Test
    void acceptsCourseEmailExamples() {
        for (String email : new String[]{"student@baylor.edu", "alice@example.com"}) {
            assertDoesNotThrow(() -> validator.validate(applicant(email, "secret phrase")));
        }
    }

    @Test
    void rejectsMalformedEmailExamples() {
        for (String email : new String[]{"studentgmail.com", "student@", "@gmail.com", "", "   ",
                "stu dent@gmail.com", "student@g mail.com"}) {
            assertThrows(ValidationException.class, () -> validator.validate(applicant(email, "secret phrase")));
        }
    }

    @Test
    void missingPasswordRejected() {
        for (String password : new String[]{null, "", "   "}) {
            assertThrows(ValidationException.class, () -> validator.validate(applicant("person@example.com", password)));
        }
    }

    @Test
    void missingAndAdministrativeRolesRejected() {
        for (Role role : new Role[]{null, Role.ADMINISTRATOR}) {
            assertThrows(ValidationException.class, () -> validator.validate(
                    new RegisterAccountRequest("person@example.com", "secret phrase", role, null, null)));
        }
    }

    @Test
    void recruiterCompanyRequired() {
        for (String company : new String[]{null, "", "  "}) {
            assertThrows(ValidationException.class, () -> validator.validate(
                    new RegisterAccountRequest("hr@example.com", "secret phrase", Role.RECRUITER, company, "Rhea")));
        }
    }

    @Test
    void recruiterNameRequired() {
        for (String name : new String[]{null, "", "  "}) {
            assertThrows(ValidationException.class, () -> validator.validate(
                    new RegisterAccountRequest("hr@example.com", "secret phrase", Role.RECRUITER, "Baylor Tech", name)));
        }
    }

    @Test
    @DisplayName("BCrypt input limit is measured in UTF-8 bytes, including multibyte characters")
    void passwordByteLimit() {
        assertDoesNotThrow(() -> validator.validate(applicant("person@example.com", "x".repeat(72))));
        assertDoesNotThrow(() -> validator.validate(applicant("person@example.com", "é".repeat(36))));
        assertThrows(ValidationException.class, () -> validator.validate(applicant("person@example.com", "x".repeat(73))));
        assertThrows(ValidationException.class, () -> validator.validate(applicant("person@example.com", "é".repeat(37))));
    }

    @Test
    void passwordMinimumAcceptsEightCharactersAndLongerPhrases() {
        var error = assertThrows(ValidationException.class,
                () -> validator.validate(applicant("person@example.com", "abcdefg")));
        assertEquals("Password must contain at least 8 Unicode characters.", error.getMessage());
        assertFalse(error.getMessage().contains("abcdefg"));
        assertDoesNotThrow(() -> validator.validate(applicant("person@example.com", "abcdefgh")));
        assertDoesNotThrow(() -> validator.validate(applicant("person@example.com", "a longer password phrase")));
    }

    @Test
    void passwordMinimumCountsSupplementaryCodePointsOnce() {
        assertThrows(ValidationException.class,
                () -> validator.validate(applicant("person@example.com", "𠮷".repeat(7))));
        assertDoesNotThrow(() -> validator.validate(applicant("person@example.com", "𠮷".repeat(8))));
    }

    @Test
    void supplementaryPasswordsRespectUtf8ByteBoundary() {
        assertDoesNotThrow(() -> validator.validate(applicant("person@example.com", "𠮷".repeat(18))));
        var error = assertThrows(ValidationException.class,
                () -> validator.validate(applicant("person@example.com", "𠮷".repeat(19))));
        assertEquals("Password must not exceed 72 UTF-8 bytes.", error.getMessage());
    }

    @Test
    void passwordSpacesAndUnicodeArePreservedExactly() {
        String password = "  𠮷abcde  ";
        var request = applicant("person@example.com", password);
        assertDoesNotThrow(() -> validator.validate(request));
        assertEquals(password, request.password());
        assertThrows(ValidationException.class,
                () -> validator.validate(applicant("person@example.com", " ".repeat(8))));
        assertThrows(ValidationException.class,
                () -> validator.validate(applicant("person@example.com", "\u2003".repeat(8))));
    }

    @Test
    void recruiterNameLimitCountsTrimmedUnicodeCodePoints() {
        for (String character : new String[]{"x", "𠮷"}) {
            assertDoesNotThrow(() -> validator.validate(new RegisterAccountRequest(
                    "hr@example.com", "secret phrase", Role.RECRUITER, "Baylor Tech", " " + character.repeat(100) + " ")));
            var error = assertThrows(ValidationException.class, () -> validator.validate(new RegisterAccountRequest(
                    "hr@example.com", "secret phrase", Role.RECRUITER, "Baylor Tech", character.repeat(101))));
            assertEquals("Recruiter name must not exceed 100 Unicode characters.", error.getMessage());
        }
    }

    @Test
    void recruiterNamesAllowSpacesPunctuationAndUnicode() {
        for (String name : new String[]{"Anne-Marie O'Connor", "李 明", "𠮷田 太郎"}) {
            var request = new RegisterAccountRequest("hr@example.com", "secret phrase", Role.RECRUITER,
                    "Baylor Tech", " " + name + " ");
            assertDoesNotThrow(() -> validator.validate(request));
            assertEquals(name, request.recruiterName());
        }
    }

    @Test
    void companyNameLimitAppliesAfterTrimming() {
        assertDoesNotThrow(() -> validator.validate(new RegisterAccountRequest(
                "hr@example.com", "secret phrase", Role.RECRUITER, " " + "x".repeat(150) + " ", "Rhea")));
        var error = assertThrows(ValidationException.class, () -> validator.validate(new RegisterAccountRequest(
                "hr@example.com", "secret phrase", Role.RECRUITER, "x".repeat(151), "Rhea")));
        assertEquals("Company name must not exceed 150 UTF-16 units.", error.getMessage());
    }

    @Test
    void companyNamesAllowPunctuationAndUnicodeWithinExistingLengthConvention() {
        for (String name : new String[]{"L'Étoile & Sons, Inc.", "科技".repeat(75), "𠮷".repeat(75)}) {
            var request = new RegisterAccountRequest("hr@example.com", "secret phrase", Role.RECRUITER,
                    " " + name + " ", "Rhea");
            assertDoesNotThrow(() -> validator.validate(request));
            assertEquals(name, request.companyName());
        }
        assertThrows(ValidationException.class, () -> validator.validate(new RegisterAccountRequest(
                "hr@example.com", "secret phrase", Role.RECRUITER, "𠮷".repeat(76), "Rhea")));
    }

    @Test
    void mappedFieldLengthsRespected() {
        assertThrows(ValidationException.class, () -> validator.validate(applicant("x".repeat(250) + "@example.com", "secret phrase")));
        assertThrows(ValidationException.class, () -> validator.validate(new RegisterAccountRequest(
                "hr@example.com", "secret phrase", Role.RECRUITER, "x".repeat(256), "Rhea")));
        assertThrows(ValidationException.class, () -> validator.validate(new RegisterAccountRequest(
                "hr@example.com", "secret phrase", Role.RECRUITER, "Baylor Tech", "x".repeat(256))));
    }

    @Test
    void validationErrorsAndToStringDoNotExposePassword() {
        String password = "private".repeat(12);
        RegisterAccountRequest request = applicant("invalid", password);
        ValidationException error = assertThrows(ValidationException.class, () -> validator.validate(request));
        assertFalse(error.getMessage().contains(password));
        assertFalse(request.toString().contains(password));
    }
}
