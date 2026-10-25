package edu.baylor.csi5324.rams.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Locale;

/** Normalized registration input; the password is write-only in JSON and redacted in diagnostics. */
public record RegisterAccountRequest(
        @NotBlank(message = "Provide an email address.")
        @Email(message = "Provide a valid email address.")
        @Size(max = 255, message = "Email must not exceed 255 characters.") String email,
        @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
        @NotBlank(message = "Provide a password.") String password,
        @NotNull(message = "Select APPLICANT or RECRUITER.") Role role,
        @Size(max = 150, message = "Company name must not exceed 150 UTF-16 units.") String companyName,
        String recruiterName,
        @Size(max = 100, message = "Applicant name must not exceed 100 characters.") String applicantName) {

    /** Registration without an applicant name (the name then defaults to the email's local part). */
    public RegisterAccountRequest(String email, String password, Role role, String companyName, String recruiterName) {
        this(email, password, role, companyName, recruiterName, null);
    }

    public RegisterAccountRequest {
        email = email == null ? null : email.trim().toLowerCase(Locale.ROOT);
        companyName = companyName == null ? null : companyName.trim();
        recruiterName = recruiterName == null ? null : recruiterName.trim();
        applicantName = applicantName == null || applicantName.isBlank() ? null : applicantName.trim();
        // Passwords are deliberately not trimmed or otherwise normalized.
    }

    /** A record's generated toString would expose the plaintext credential. */
    @Override
    public String toString() {
        return "RegisterAccountRequest[role=" + role + ", password=<redacted>]";
    }
}
