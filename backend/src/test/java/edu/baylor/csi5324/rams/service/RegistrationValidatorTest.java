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
        return new RegisterAccountRequest(email, password, Role.APPLICANT, null, "Ann Lee");
    }

    @Test
    void missingAndInvalidEmailRejected() {
        for (String email : new String[]{null, "", "studentgmail.com", "a@@example.com"}) {
            assertThrows(ValidationException.class, () -> validator.validate(applicant(email, "secret phrase")));
        }
    }

    @Test
    void missingPasswordRejected() {
        for (String password : new String[]{null, "", " ".repeat(8)}) {
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
        for (String company : new String[]{null, "  "}) {
            assertThrows(ValidationException.class, () -> validator.validate(
                    new RegisterAccountRequest("hr@example.com", "secret phrase", Role.RECRUITER, company, "Rhea")));
        }
    }

    @Test
    void fullNameRequiredForBothRoles() {
        for (String name : new String[]{null, "  "}) {
            assertThrows(ValidationException.class, () -> validator.validate(
                    new RegisterAccountRequest("person@example.com", "secret phrase", Role.APPLICANT, null, name)));
            assertThrows(ValidationException.class, () -> validator.validate(
                    new RegisterAccountRequest("hr@example.com", "secret phrase", Role.RECRUITER, "Baylor Tech", name)));
        }
    }

    @Test
    @DisplayName("BCrypt input limit is measured in UTF-8 bytes, including multibyte characters")
    void passwordByteLimit() {
        assertDoesNotThrow(() -> validator.validate(applicant("person@example.com", "x".repeat(72))));
        assertThrows(ValidationException.class, () -> validator.validate(applicant("person@example.com", "x".repeat(73))));
        assertDoesNotThrow(() -> validator.validate(applicant("person@example.com", "𠮷".repeat(18))));
        var error = assertThrows(ValidationException.class,
                () -> validator.validate(applicant("person@example.com", "𠮷".repeat(19))));
        assertEquals("Password must not exceed 72 UTF-8 bytes.", error.getMessage());
    }

    @Test
    void passwordMinimumRejectsSevenAndAcceptsEightCharacters() {
        var error = assertThrows(ValidationException.class,
                () -> validator.validate(applicant("person@example.com", "abcdefg")));
        assertEquals("Password must contain at least 8 Unicode characters.", error.getMessage());
        assertFalse(error.getMessage().contains("abcdefg"));
        assertDoesNotThrow(() -> validator.validate(applicant("person@example.com", "abcdefgh")));
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
